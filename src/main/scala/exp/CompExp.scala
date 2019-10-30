package exp


import scala.collection.JavaConversions._
import scalax.collection.Graph
import scalax.collection.edge.Implicits._
import sdrlift.graph.Dfg._
import sdrlift.graph.{DfgEdge, DfgNode, NodeFactory}
import sdrlift.graph.NodeFactory._
import NodeExp._
import exp.CompExp.DummyComp

import scala.collection.generic.{CanBuildFrom, GenericTraversableTemplate, SeqFactory}
import scala.collection.{Iterator, Seq, SeqLike}
import scala.collection.immutable.Map
import scala.collection.mutable.ListBuffer


object CompExp {

  abstract class Component extends Exp {
    val inst: String
    val name: String

    def dfg = Graph.empty[DfgNode, DfgEdge]
    var _params: Map[String, Any] = Map.empty
    var _inports: List[Streamer] = List.empty
    var _outports: List[Streamer] = List.empty
    var _inlinks: List[(Node, Streamer, Node, Streamer)] = List()
    var _outlinks: List[(Node, Streamer, Node, Streamer)] = List()
    val iopaths = List.empty[(Streamer, Streamer)]

    private def getArithComponent(lhs: Component, rhs: Component, op: String): Component = {
      def getArithArgNode(c: Component) = c match {
        case comp: Comp => c.dfg.getLeaves.head
        case dummyComp: DummyComp => c.asInstanceOf[DummyComp].n
        case _ => {
          if (c.dfg.isEmpty)
            TmplNode(c)
          else
            CompNode(c)
        }
      }

      val lhsNode: Node = getArithArgNode(lhs)
      val lhsId = if(lhsNode.isInstanceOf[ArithNode]) lhsNode.name else lhsNode.inst
      val rhsNode: Node = getArithArgNode(rhs)
      val rhsId = if(rhsNode.isInstanceOf[ArithNode]) rhsNode.name else rhsNode.inst

      val arithNode = ArithNode(op + "_" + lhsId + "_" + rhsId, getNodeExp(lhsNode),
        getNodeExp(rhsNode), getArith2ArgNodeWidth(op, lhsNode.width, rhsNode.width), op, 1) //lhsNode.level + 1)


      /* if(op.equals("add")){
        println(arithNode.inst + " " + arithNode.width)
        println(lhsNode + " " + lhsNode.width)
        println(rhsNode + " " + rhsNode.width)
        println
      } */

      val lhsEdge = DfgEdge[DfgNode](lhsNode, arithNode, lhsNode.inst + "_" + arithNode.name, null, null)
      val rhsEdge = DfgEdge[DfgNode](rhsNode, arithNode, rhsNode.inst + "_" + arithNode.name, null, null)

      Comp(inst, op + "_" + inst, Graph[DfgNode, DfgEdge](lhsEdge, rhsEdge))
    }

    def +(rhs: Component): Component = {
      getArithComponent(this, rhs, "add")
    }

    def *(rhs: Component): Component = {
      getArithComponent(this, rhs, "mul")
    }

    def inLinks(lnks: Streamer*): Component = {
      lnks.foreach { lnk =>
        val link2Arg = lnk.logicExp.get.asInstanceOf[Link2Arg]

        val srcNode =
          if(link2Arg.src.cmp == null){
            val g = model(Seq(link2Arg.src))
            g.getLeaves.head
          }else {
            if(!link2Arg.src.cmp.dfg.isEmpty)
              CompNode(link2Arg.src.cmp)
            else
              TmplNode(link2Arg.src.cmp)
          }

        val tgtNode =
          if(link2Arg.tgt.cmp == null){
            StrmNode(link2Arg.tgt)
          }else {
            if(!link2Arg.tgt.cmp.dfg.isEmpty)
              CompNode(link2Arg.tgt.cmp)
            else
              TmplNode(link2Arg.tgt.cmp)
          }

        _inlinks = _inlinks ::: List((srcNode, link2Arg.src, tgtNode, link2Arg.tgt))
      }
      this
    }

    def outLinks(lnks: Streamer*): Component = {
      lnks.foreach { lnk =>

        val link2Arg = lnk.logicExp.get.asInstanceOf[Link2Arg]

        val srcNode =
          if(link2Arg.src.cmp == null){
            val g = model(Seq(link2Arg.src))
            g.getLeaves.head
          }else {
            if(!link2Arg.src.cmp.dfg.isEmpty)
              CompNode(link2Arg.src.cmp)
            else
              TmplNode(link2Arg.src.cmp)
          }

        val tgtNode =
          if(link2Arg.tgt.cmp == null){
            StrmNode(link2Arg.tgt)
          }else {
            if(!link2Arg.tgt.cmp.dfg.isEmpty)
              CompNode(link2Arg.tgt.cmp)
            else
              TmplNode(link2Arg.tgt.cmp)
          }
        _outlinks = _outlinks ::: List((srcNode, link2Arg.src, tgtNode, link2Arg.tgt))
      }
      this
    }

    def parseExp(lvl: Int, exp: Option[Any]): List[DfgEdge[DfgNode]] = (exp) match {

      case (Some(Combinational(_))) => {
        val cmbExp = exp.get.asInstanceOf[Combinational]
        val cmbNode = DirNode("cmb_" + cmbExp._name, "Combinational")
        var list: List[DfgEdge[DfgNode]] = List()

        cmbExp._codeBlock.head.asInstanceOf[List[Any]].foreach { sn =>
          (sn) match {
            case (stm: Streamer) => { // Streamer statements
              if(stm.lvl == 0) {
                val stmNode = StrmNode(stm)

                list = list ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode](stmNode, cmbNode,
                  stmNode.name + "_" + cmbNode.name, null, null))
              }else{
                val combCodeBlockList = parseExp(stm.lvl, stm.logicExp)
                var cmbGrph = Graph.empty[DfgNode, DfgEdge]

                cmbGrph = cmbGrph ++ combCodeBlockList
                val cmbComp = new Comp("cmbCompInst", "cmbComp", cmbGrph)
                val cmbGrphEndNode = cmbComp.dfg.getGraphEndNode

                list = list ::: combCodeBlockList

                list = list ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode](cmbGrphEndNode, cmbNode,
                  cmbGrphEndNode.name + "_" + cmbNode.name, null, null))
              }
            }
            case _ => {
              val comp = sn.asInstanceOf[Component]
              val cNode = if (comp.dfg.isEmpty) TmplNode(comp) else CompNode(comp)


              comp._inlinks.foreach { lnk =>
                list = list ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode]
                  (lnk._1, cNode, lnk._1.inst  + "_" + lnk._2.name + "_" + cNode.inst + "_" + lnk._4.name, lnk._2, lnk._4))
              }

              comp._outlinks.foreach { lnk =>
                list = list ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode]
                  (cNode, lnk._3, cNode.inst  + "_" + lnk._2.name + "_"  + lnk._1.inst + "_" + lnk._4.name,  lnk._2, lnk._4))

                list = list ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode](list.last.target, cmbNode,
                  list.last.target.inst + "_" + cmbNode.name, null, null))
              }

            }
          }
        }

        list
      }

      case (Some(Sequential(_))) => {
        val seqExp = exp.get.asInstanceOf[Sequential]
        val seqNode = DirNode("seq_" + seqExp._name, "Sequential")
        var list: List[DfgEdge[DfgNode]] = List()

        seqExp._codeBlock.head.asInstanceOf[List[Any]].foreach { sn =>
          (sn) match {
            case (strm: Streamer) => { // Streamer statements
              val seqCodeBlockList = parseExp(strm.lvl, strm.logicExp)

              var seqGrph = Graph.empty[DfgNode, DfgEdge]
              seqGrph = seqGrph ++ seqCodeBlockList
              val cmbComp = new Comp("seqCompInst", "seqComp", seqGrph)
              val cmbGrphEndNode = cmbComp.dfg.getGraphEndNode

              list = list ::: seqCodeBlockList :::
                List[DfgEdge[DfgNode]](DfgEdge[DfgNode](cmbGrphEndNode, seqNode, cmbGrphEndNode.name + "_" + seqNode.name, null, null))
            }
            case (fsmExp: Fsm) => { // Fsm statement
              val seqCodeBlockList = parseExp(0, Some(fsmExp))

              var seqGrph = Graph.empty[DfgNode, DfgEdge]
              seqGrph = seqGrph ++ seqCodeBlockList
              val cmbComp = new Comp("seqCompInst", "seqComp", seqGrph)
              val cmbGrphEndNode = cmbComp.dfg.getGraphEndNode

              list = list ::: seqCodeBlockList :::
                List[DfgEdge[DfgNode]](DfgEdge[DfgNode](cmbGrphEndNode, seqNode, cmbGrphEndNode.name + "_" + seqNode.name, null, null))
            }
            case (ifExp: If) => { // Fsm statement
              val seqCodeBlockList = parseExp(0, Some(ifExp))

              var seqGrph = Graph.empty[DfgNode, DfgEdge]
              seqGrph = seqGrph ++ seqCodeBlockList
              val cmbComp = new Comp("seqCompInst", "seqComp", seqGrph)
              val cmbGrphEndNode = cmbComp.dfg.getGraphEndNode

              list = list ::: seqCodeBlockList :::
                List[DfgEdge[DfgNode]](DfgEdge[DfgNode](cmbGrphEndNode, seqNode, cmbGrphEndNode.name + "_" + seqNode.name, null, null))
            }
            case _ =>
          }
        }

        list
      }

      case (Some(Arith2Arg(_, _, _))) => {

        val arithExp = exp.get.asInstanceOf[Arith2Arg]

        (lvl, arithExp.lhs, arithExp.rhs) match {

          case (1, stm1: Streamer, stm2: Streamer) => {

            // arithNode
            val arithNode =
              (stm1.cmp, stm2.cmp) match {
                case (null, null) => ArithNode(arithExp.prfx + "_" + stm1.name + "_" + stm2.name, stm1, stm2, getArith2ArgNodeWidth(arithExp.prfx, stm1.width, stm2.width), arithExp.prfx, 1)
                case (_, null) => ArithNode(arithExp.prfx + "_" + stm1.cmp.inst + "_" + stm1.name + "_" + stm2.name, stm1.cmp, stm2, getArith2ArgNodeWidth(arithExp.prfx, stm1.width, stm2.width), arithExp.prfx, 1)
                case (null, _) => ArithNode(arithExp.prfx + "_" + stm1.name + "_" + stm2.cmp.inst + "_" + stm2.name, stm1, stm2.cmp, getArith2ArgNodeWidth(arithExp.prfx, stm1.width, stm2.width), arithExp.prfx, 1)
                case (_,_) => ArithNode(arithExp.prfx + "_" + stm1.cmp.inst + "_" + stm1.name + "_" + stm2.cmp.inst + "_" + stm2.name, stm1.cmp, stm2.cmp, getArith2ArgNodeWidth(arithExp.prfx, stm1.width, stm2.width), arithExp.prfx, 1)
              }



            val e1 = if (stm1.cmp == null)
              DfgEdge[DfgNode](StrmNode(stm1), arithNode, stm1.name + "_" + arithNode.name, null, null)
            else {
              if (!stm1.cmp.dfg.isEmpty)
                DfgEdge[DfgNode](CompNode(stm1.cmp), arithNode, stm1.cmp.inst + "_" + arithNode.name, stm1, null)
              else
                DfgEdge[DfgNode](TmplNode(stm1.cmp), arithNode, stm1.cmp.inst + "_" + arithNode.name, stm1, null)
            }

            val e2 = if (stm2.cmp == null)
              DfgEdge[DfgNode](StrmNode(stm2), arithNode, stm2.name + "_" + arithNode.name, null, null)
            else {
              if (!stm2.cmp.dfg.isEmpty)
                DfgEdge[DfgNode](CompNode(stm2.cmp), arithNode, stm2.cmp.inst + "_" + arithNode.name, stm2, null)
              else
                DfgEdge[DfgNode](TmplNode(stm2.cmp), arithNode, stm2.cmp.inst + "_" + arithNode.name, stm2, null)
            }

            List(e1, e2)
          }

          case (1, stm1: Streamer,  stm2: Const) => {
            val arithNode = ArithNode(arithExp.prfx + "_" + stm1.name + "_ct_" + stm2.lvl, stm1, stm2,
              getArith2ArgNodeWidth(arithExp.prfx, stm1.width, stm2.width), arithExp.prfx, 1)

            val ch1Label = stm1.name + "_" + arithNode.name
            val ch2Label = "ct_" + stm2.lvl + "_" + arithNode.name
            val e1 = DfgEdge[DfgNode](StrmNode(stm1), arithNode, ch1Label, null, null)
            val e2 = DfgEdge[DfgNode](ConstNode(stm2.vl, stm2.lvl), arithNode, ch2Label, null, null)

            List(e1, e2)
          }

          case (_,  stm1: Streamer,  stm2: Streamer) => {

            val arithList1 = parseExp(stm1.lvl, stm1.logicExp)
            val recArithNode1 = if (arithList1 != null) arithList1.head.target else StrmNode(stm1)

            val arithList2 = parseExp(stm2.lvl, stm2.logicExp)
            val recArithNode2 = if (arithList2 != null) arithList2.head.target else StrmNode(stm2)

            val dfg = Graph.empty[DfgNode, DfgEdge]
            val arithNode = ArithNode(arithExp.prfx + "_" + recArithNode1.name + "_" + recArithNode2.name,
              getNodeExp(recArithNode1), getNodeExp(recArithNode2), getArith2ArgNodeWidth(arithExp.prfx, recArithNode1.width,
                recArithNode2.width), arithExp.prfx, 1)

            val ch1Label = recArithNode1.name + "_" + arithNode.name
            val ch2Label = recArithNode2.name + "_" + arithNode.name
            val l1 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](recArithNode1, arithNode, ch1Label, null, null)) ::: (if (arithList1 != null) arithList1 else List())
            val l2 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](recArithNode2, arithNode, ch2Label, null, null)) ::: (if (arithList2 != null) arithList2 else List())

            l1 ::: l2
          }

          case (_,  stm: Streamer,  const: Const) => {

            val arithList1 = parseExp(stm.lvl, stm.logicExp)

            val recArithNode1 = arithList1.head.target
            val arithNode = ArithNode(arithExp.prfx + "_" + recArithNode1.name + "_ct_" + const.vl, getNodeExp(recArithNode1),
               const, getArith2ArgNodeWidth(arithExp.prfx, recArithNode1.width, const.width), arithExp.prfx, 1)

            val ch1Label = recArithNode1.name + "_" + arithNode.name
            val ch2Label = "ct_" + const.vl + "_" + arithNode.name
            val l1 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](recArithNode1, arithNode, ch1Label, null, null)) ::: arithList1
            val l2 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](ConstNode(const.vl, const.lvl), arithNode, ch2Label, null, null)) //::: List

            l1 ::: l2
          }

          case (_,  cmp: Component,  dummy: DummyComp) => {
            val cmpLeafNode =  if(cmp.dfg != null) CompNode(cmp) else TmplNode(cmp) //cmp.dfg.getLeaves.head
            val dummyNode = dummy.n
            val arithNode = ArithNode(arithExp.prfx + "_" + cmpLeafNode.inst + "_" + dummyNode.inst, getNodeExp(cmpLeafNode), getNodeExp(dummyNode), (if(cmpLeafNode.width > dummy.n.width) cmpLeafNode.width else dummy.n.width), arithExp.prfx,1)

            val ch1Label = cmpLeafNode.inst  + "_" + arithNode.name
            val ch2Label = dummyNode.inst + "_" + arithNode.name
            val l1 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](cmpLeafNode, arithNode, ch1Label, null, null))
            val l2 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](dummyNode, arithNode, ch2Label, null, null))
            l1 ::: l2
          }

          case (_,  cmp1: Component,  cmp2: Component) => {
            val cmpLeafNode1 = if(cmp1.dfg != null) CompNode(cmp1) else TmplNode(cmp1) //cmp1.dfg.getLeaves.head
            val cmpLeafNode2 = if(cmp2.dfg != null) CompNode(cmp2) else TmplNode(cmp2)
            val arithNode = ArithNode(arithExp.prfx + "_" + cmpLeafNode1.inst + "_" + cmpLeafNode2.inst, cmp1, cmp2, getArith2ArgNodeWidth(arithExp.prfx, cmp1.width,cmp2.width), arithExp.prfx, 1)
            val ch1Label = cmpLeafNode1.inst  + "_" + arithNode.name
            val ch2Label = cmpLeafNode2.inst + "_" + arithNode.name
            val l1 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](cmpLeafNode1, arithNode, ch1Label, null, null))
            val l2 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](cmpLeafNode2, arithNode, ch2Label, null, null))
            l1 ::: l2
          }
          case (_,  arg1: Arith2Arg,  arg2: Arith2Arg) => {
            val arithList1 =  parseExp(arg1.level, Some(arg1))
            val recArithNode1 = arithList1.head.target

            val arithList2 = parseExp(arg2.level, Some(arg2))
            val recArithNode2 = arithList2.head.target

            val arithNode = ArithNode(arithExp.prfx + "_" + recArithNode1.name + "_" + recArithNode2.name,
              getNodeExp(recArithNode1), getNodeExp(recArithNode2), getArith2ArgNodeWidth(arithExp.prfx, recArithNode1.width,
                recArithNode2.width), arithExp.prfx, 1)

            val ch1Label = recArithNode1.name + "_" + arithNode.name
            val ch2Label = recArithNode2.name + "_" + arithNode.name
            val l1 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](recArithNode1, arithNode, ch1Label, null, null)) ::: (if (arithList1 != null) arithList1 else List())
            val l2 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](recArithNode2, arithNode, ch2Label, null, null)) ::: (if (arithList2 != null) arithList2 else List())

            l1 ::: l2
          }

          //case _ => null
        }
      }

      case (Some(Logi2Arg(_, _, _))) => {
        val lgcExp = exp.get.asInstanceOf[Logi2Arg]
        (lvl, lgcExp.lhs, lgcExp.rhs) match {
          case (1, stm1: Streamer, stm2: Streamer) => {
            val lgcNode = LogiNode(lgcExp.prfx + "_" + stm1.name + "_" + stm2.name, stm1, stm2,
              getArith2ArgNodeWidth(lgcExp.prfx, stm1.width, stm2.width), lgcExp.prfx, lgcExp.level)

            val ch1Label = stm1.name + "_" + lgcNode.name
            val ch2Label = stm2.name + "_" + lgcNode.name
            val e1 = DfgEdge[DfgNode](StrmNode(stm1), lgcNode, ch1Label, null, null)
            val e2 = DfgEdge[DfgNode](StrmNode(stm2), lgcNode, ch2Label, null, null)

            List(e1, e2)
          }

          case (1, stm: Streamer, const: Const) => {
            val lgcNode = LogiNode(lgcExp.prfx + "_" + stm.name + "_ct_" + const.vl, stm,  const,
              getArith2ArgNodeWidth(lgcExp.prfx, stm.width, const.width), lgcExp.prfx, lgcExp.level)

            val ch1Label = stm.name + "_" + lgcNode.name
            val ch2Label = "ct_" + const.vl + "_" + lgcNode.name
            val e1 = DfgEdge[DfgNode](StrmNode(stm), lgcNode, ch1Label, null, null)
            val e2 = DfgEdge[DfgNode](ConstNode(const.vl, const.lvl), lgcNode, ch2Label, null, null)

            List(e1, e2)
          }

          case (_, stm1: Streamer, stm2: Streamer) => {
            var lgcList1 = parseExp(stm1.lvl, stm1.logicExp)
            val recLgcNode1 = if (lgcList1 != null) lgcList1.head.target else StrmNode(stm1)

            var lgcList2 = parseExp(stm2.lvl, stm2.logicExp)
            val recLgcNode2 = if (lgcList2 != null) lgcList2.head.target else StrmNode(stm2)

            val sufx = if(recLgcNode1.name.equals(recLgcNode2.name)) recLgcNode1.name else recLgcNode1.name + "_" + recLgcNode2.name
            val lgcNode = LogiNode(lgcExp.prfx + "_" + sufx, getNodeExp(recLgcNode1),
              getNodeExp(recLgcNode2), getArith2ArgNodeWidth(lgcExp.prfx, recLgcNode1.width, recLgcNode2.width), lgcExp.prfx,
              lgcExp.level)

            lgcList1 = if (lgcList1 != null) lgcList1 else List()
            lgcList2 = if (lgcList2 != null) lgcList2 else List()
            val l1 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](recLgcNode1, lgcNode, recLgcNode1.name + "_" + lgcNode.name, null, null)) ::: lgcList1
            val l2 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](recLgcNode2, lgcNode, recLgcNode2.name + "_" + lgcNode.name, null, null)) ::: lgcList2

            l1 ::: l2
          }

          case (_, stm: Streamer, const: Const) => {
            var lgcList = parseExp(stm.lvl, stm.logicExp)

            val recLgcNode = if (lgcList != null) lgcList.head.target else StrmNode(stm)
            val lgcNode = LogiNode(lgcExp.prfx + "_" + recLgcNode.name + "_ct_" + const.vl,
              getNodeExp(recLgcNode), const, getArith2ArgNodeWidth(lgcExp.prfx, recLgcNode.width,
                const.width), lgcExp.prfx, lgcExp.level)

            lgcList = if (lgcList != null) lgcList else List()
            val l1 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](recLgcNode, lgcNode, recLgcNode.name + "_" + lgcNode.name, null, null)) ::: lgcList
            val l2 = List[DfgEdge[DfgNode]](DfgEdge[DfgNode](ConstNode(const.vl, const.lvl), lgcNode, "ct_" + const.vl + "_" + lgcNode.name, null, null))

            l1 ::: l2
          }
          case (_, stm: Streamer, null) => {
            val lgcNode = LogiNode(lgcExp.prfx + "_" + stm.name , stm, stm, stm.width, lgcExp.prfx, lgcExp.level)
            List(DfgEdge[DfgNode](StrmNode(stm), lgcNode, stm.name + "_" + lgcNode.name, null, null))
          }
          //case _ => null
        }
      }

      case (Some(Link2Arg(_, _))) => {
        val lnk2Arg = exp.get.asInstanceOf[Link2Arg]
        (lnk2Arg.src, lnk2Arg.tgt) match {
          case (stm1: Streamer, stm2: Streamer) => {
            var srcPort: Streamer = stm1
            var snkPort: Streamer = stm2
            var lnkList1 = parseExp(stm1.lvl, stm1.logicExp)
            val recLnkNode1 =
              if (lnkList1 != null) {
                if (lnk2Arg.src.cmp == null) {
                  var grph = Graph.empty[DfgNode, DfgEdge]
                  grph = grph ++ lnkList1
                  grph.getGraphEndNode
                }else {
                  if (!lnk2Arg.src.cmp.dfg.isEmpty)
                    CompNode(lnk2Arg.src.cmp)
                  else
                    TmplNode(lnk2Arg.src.cmp)
                }
              } else if (lnk2Arg.src.cmp != null) {
                if (!lnk2Arg.src.cmp.dfg.isEmpty)
                  CompNode(lnk2Arg.src.cmp)
                else
                  TmplNode(lnk2Arg.src.cmp)
              }
              else StrmNode(stm1)

            var lnkList2 = parseExp(stm2.lvl, stm2.logicExp)
            val recLnk2Node2 =
              if (lnkList2 != null) {
                if (lnk2Arg.tgt.cmp == null) {
                  var grph = Graph.empty[DfgNode, DfgEdge]
                  grph = grph ++ lnkList2
                  grph.getGraphEndNode
                }else {
                  if (!lnk2Arg.tgt.cmp.dfg.isEmpty)
                    CompNode(lnk2Arg.tgt.cmp)
                  else
                    TmplNode(lnk2Arg.tgt.cmp)
                }
              } else if (lnk2Arg.tgt.cmp != null) {
                if (!lnk2Arg.tgt.cmp.dfg.isEmpty)
                  CompNode(lnk2Arg.tgt.cmp)
                else
                  TmplNode(lnk2Arg.tgt.cmp)
              }
              else StrmNode(stm2)

            lnkList1 = if (lnkList1 != null) lnkList1 else List()
            lnkList2 = if (lnkList2 != null) lnkList2 else List()
            List[DfgEdge[DfgNode]](DfgEdge[DfgNode](recLnkNode1, recLnk2Node2, recLnkNode1.name + "_" + recLnk2Node2.name, srcPort, snkPort))
          }
        }
      }

      case (Some(Fsm(_))) => {
        val fsmExp = exp.get.asInstanceOf[Fsm]
        val fsmNode = FsmNode(fsmExp._name, fsmExp._states.indexOf(fsmExp._firstState), 0)

        var stateList: List[StateNode] = List()
        var list: List[DfgEdge[DfgNode]] = List()

        // add states
        fsmExp._states.foreach { st =>
          val stateNode = StateNode(st.name, fsmExp._states.indexOf(st))
          st._task.foreach { sn =>
            (sn) match {
              case (stm: Streamer) => {
                val fsmCodeBlockList = parseExp(stm.lvl, stm.logicExp)

                var fsmGrph = Graph.empty[DfgNode, DfgEdge]
                fsmGrph = fsmGrph ++ fsmCodeBlockList
                val fsmComp = new Comp("fsmCompInst", "fsmComp", fsmGrph)
                val fsmGrphEndNode = fsmComp.dfg.getGraphEndNode

                if (stateList.size > 0) {
                  list = list :::
                    List[DfgEdge[DfgNode]](DfgEdge[DfgNode](stateNode, stateList.last, stateNode.name + "_" + stateList.last.name, null, null)) :::
                    List[DfgEdge[DfgNode]](DfgEdge[DfgNode](fsmGrphEndNode, stateNode, fsmGrphEndNode.name + "_" + stateNode.name, null, null))
                } else {
                  list = list :::
                    List[DfgEdge[DfgNode]](DfgEdge[DfgNode](stateNode, fsmNode, stateNode.name + "_" + fsmNode.name, null, null)) :::
                    List[DfgEdge[DfgNode]](DfgEdge[DfgNode](fsmGrphEndNode, stateNode, fsmGrphEndNode.name + "_" + stateNode.name, null, null))
                }
                list = list ::: fsmCodeBlockList
                stateList = stateList ::: List(stateNode)
              }
              case (ifn: If) => {
                // The If has other sub-If statement
                list = list ::: parseIfExp(ifn, stateNode)
              }
              case _ =>
            }
          }

          // add next state condition
          val trnList = st._to.toList
          val tcList = st._transCheck.toList
          for (i <- 0 until st._to.size) {
            val tc = tcList(i)
            val tcStrmNode = tc.asInstanceOf[Streamer]
            val lgc2Arg = tcStrmNode.logicExp.get.asInstanceOf[Logi2Arg]
            val lgcNodeList = parseExp(1, Some(lgc2Arg))

            var lgcGrph = Graph.empty[DfgNode, DfgEdge]
            lgcGrph = lgcGrph ++ lgcNodeList
            val lgcComp = new Comp("lgcCompInst", "lgcComp", lgcGrph)
            val lgcGrphEndNode = lgcComp.dfg.getGraphEndNode

            val nxtStCondNode = CondNode("cond_" + lgcGrphEndNode.name,
              lgc2Arg.prfx, lgcGrphEndNode.level + 1)
            list = list ::: lgcNodeList :::
              List[DfgEdge[DfgNode]](DfgEdge[DfgNode](nxtStCondNode, stateNode, nxtStCondNode.name + "_" + stateNode.name, null, null)) :::
              List[DfgEdge[DfgNode]](DfgEdge[DfgNode](lgcGrphEndNode, nxtStCondNode, lgcGrphEndNode.name + "_" + nxtStCondNode.name, null, null))

            val trn = trnList(i)
            val trnStateNode = StateNode(trn.name, fsmExp._states.indexOf(trn))
            list = list ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode](trnStateNode, nxtStCondNode, trnStateNode.name + "_" + nxtStCondNode.name, null, null))
          }
        }

        list
      }

      case (Some(If(_))) => {
        val ifExp = exp.get.asInstanceOf[If]
        val ifStrmNode = ifExp._ifCheck.asInstanceOf[Streamer]
        val ifLgcl2Arg = ifStrmNode.logicExp.get.asInstanceOf[Logi2Arg]
        val ifNode = IfNode("if_" + ifExp._name, 0)

        var list: List[DfgEdge[DfgNode]] = List()
        val ifLgcNodeList = parseExp(ifStrmNode.lvl, Some(ifLgcl2Arg))

        var ifLgcGrph = Graph.empty[DfgNode, DfgEdge]
        ifLgcGrph = ifLgcGrph ++ ifLgcNodeList
        val ifLgcComp = new Comp("ifLgcCompInst", "ifLgcComp", ifLgcGrph)
        val ifLgcGrphEndNode = ifLgcComp.dfg.getGraphEndNode

        val ifCondNode = CondNode("cond_" + ifLgcGrphEndNode.name,
          ifLgcl2Arg.prfx, ifLgcGrphEndNode.level + 1)

        list = list ::: ifLgcNodeList :::
          List[DfgEdge[DfgNode]](DfgEdge[DfgNode](ifCondNode, ifNode, ifCondNode.name + "_" + ifNode.name, null, null)) :::
          List[DfgEdge[DfgNode]](DfgEdge[DfgNode](ifLgcGrphEndNode, ifCondNode, ifLgcGrphEndNode.name + "_" + ifCondNode.name, null, null))

        // add If-Node
        ifExp._codeBlock.foreach { sn =>
          (sn) match {
            case (strmNode: Streamer) => { // Streamer statements
              val ifCodeBlockList = parseExp(strmNode.lvl, strmNode.logicExp)

              var ifGrph = Graph.empty[DfgNode, DfgEdge]
              ifGrph = ifGrph ++ ifCodeBlockList
              val ifComp = new Comp("ifCompInst", "ifComp", ifGrph)
              val ifGrphEndNode = ifComp.dfg.getGraphEndNode

              list = list ::: ifCodeBlockList :::
                List[DfgEdge[DfgNode]](DfgEdge[DfgNode](ifGrphEndNode, ifNode, ifGrphEndNode.name + "_" + ifNode.name, null, null))
            }
            case (ifn: If) => {
              // The If has other sub-If statement
              list = list ::: parseIfExp(ifn, ifNode)

            }
            case _ =>
          }
        }

        // add else-if Statements
        var elsifList: List[ElsIfNode] = List()
        if (ifExp._elsIfs != null) {
          ifExp._elsIfs.foreach { eif =>
            val eifStrmNode = eif._2.asInstanceOf[Streamer]
            val eifLgcl2Arg = eifStrmNode.logicExp.get.asInstanceOf[Logi2Arg]
            val eifNode = ElsIfNode("elsif_" + eif._1, ifExp._elsIfs.indexOf(eif))

            val eifLgcNodeList = parseExp(eifStrmNode.lvl, Some(eifLgcl2Arg))

            var eifLgcGrph = Graph.empty[DfgNode, DfgEdge]
            eifLgcGrph = eifLgcGrph ++ eifLgcNodeList
            val eifLgcComp = new Comp("eifLgcCompInst", "eifLgcComp", eifLgcGrph)
            val eifLgcGrphEndNode = eifLgcComp.dfg.getGraphEndNode

            val eifCondNode = CondNode("cond_" + eifLgcGrphEndNode.name,
              eifLgcl2Arg.prfx, eifLgcGrphEndNode.level + 1)
            list = list ::: eifLgcNodeList :::
              List[DfgEdge[DfgNode]](DfgEdge[DfgNode](eifCondNode, eifNode, eifCondNode.name + "_" + eifNode.name, null, null)) :::
              List[DfgEdge[DfgNode]](DfgEdge[DfgNode](eifLgcGrphEndNode, eifCondNode, eifLgcGrphEndNode.name + "_" + eifCondNode.name, null, null))

            eif._3.foreach { sn =>
              (sn) match {
                case (strmNode: Streamer) => {
                  // val streamerNode = sn.asInstanceOf[Streamer]
                  val eifCodeBlockList = parseExp(strmNode.lvl, strmNode.logicExp)

                  var eifGrph = Graph.empty[DfgNode, DfgEdge]
                  eifGrph = eifGrph ++ eifCodeBlockList
                  val eifComp = new Comp("eifCompInst", "eifComp", eifGrph)
                  val eifGrphEndNode = eifComp.dfg.getGraphEndNode

                  if (elsifList.size > 0) {
                    list = list :::
                      List[DfgEdge[DfgNode]](DfgEdge[DfgNode](eifNode, elsifList.last, eifNode.name + "_" + elsifList.last.name, null, null)) :::
                      List[DfgEdge[DfgNode]](DfgEdge[DfgNode](eifGrphEndNode, eifNode, eifGrphEndNode.name + "_" + eifNode.name, null, null))
                  } else {
                    list = list :::
                      List[DfgEdge[DfgNode]](DfgEdge[DfgNode](eifNode, ifNode, eifNode.name + "_" + ifNode.name, null, null)) :::
                      List[DfgEdge[DfgNode]](DfgEdge[DfgNode](eifGrphEndNode, eifNode, eifGrphEndNode.name + "_" + eifNode.name, null, null))
                  }
                  list = list ::: eifCodeBlockList
                  elsifList = elsifList ::: List(eifNode)
                }
                case (ifn: If) => {
                  // The If has other sub-If statement
                  list = list ::: parseIfExp(ifn, eifNode)
                }
                case _ =>
              }
            }
          }
        }

        // add Else Statement
        if (ifExp._els != null) {
          val lvl = if (ifExp._elsIfs != null) ifExp._elsIfs.size + 1 else 1
          val elsNode = ElsNode("els_" + ifExp._els._1, lvl)
          ifExp._els._2.foreach { el =>
            (el) match {
              case (st: Streamer) => {
                val elsStrmNode = el.asInstanceOf[Streamer]
                val elsCodeBlockList = parseExp(st.lvl, elsStrmNode.logicExp)

                var elsGrph = Graph.empty[DfgNode, DfgEdge]
                elsGrph = elsGrph ++ elsCodeBlockList
                val elsComp = new Comp("elsCompInst", "elsComp", elsGrph)
                val elsGrphEndNode = elsComp.dfg.getGraphEndNode

                if (ifExp._elsIfs != null) // Else-Ifs exist
                  list = list ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode](elsNode, elsifList.last, elsNode.name + "_" + elsifList.last.name, null, null))
                else // No Else-Ifs
                  list = list ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode](elsNode, ifNode, elsNode.name + "_" + ifNode.name, null, null))

                list = list ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode](elsGrphEndNode, elsNode, elsGrphEndNode.name + "_" + elsNode.name, null, null))

                list = list ::: elsCodeBlockList
              }
              case (ifn: If) => {
                // The If has other sub-If statement
                list = list ::: parseIfExp(ifn, elsNode)
              }
              case _ =>
            }
          }
        }

        list
      }
      case Some(a: Integer) => {
        null
      }
      //case _ => null
    }

    def parseIfExp(ifs: If, fn: Any): List[DfgEdge[DfgNode]] = {
      var list: List[DfgEdge[DfgNode]] = List()
      val ifsExp = ifs //.asInstanceOf[If]
      val ifsStrmNode = ifsExp._ifCheck.asInstanceOf[Streamer]
      val ifsLgcl2Arg = ifsStrmNode.logicExp.get.asInstanceOf[Logi2Arg]

      val ifsLgcNodeList = parseExp(ifsStrmNode.lvl, Some(ifsLgcl2Arg))

      var ifsLgcGrph = Graph.empty[DfgNode, DfgEdge]
      ifsLgcGrph = ifsLgcGrph ++ ifsLgcNodeList
      val ifLgcComp = new Comp("ifsLgcCompInst", "ifsLgcComp", ifsLgcGrph)
      val ifsLgcGrphEndNode = ifLgcComp.dfg.getGraphEndNode

      val ifsCondNode = CondNode("cond_" + ifsLgcGrphEndNode.name,
        ifsLgcl2Arg.prfx, ifsLgcGrphEndNode.level + 1)

      val ifsNode = IfNode("if_" + ifsExp._name, 0)

      list = list ::: ifsLgcNodeList :::
        List[DfgEdge[DfgNode]](DfgEdge[DfgNode](ifsCondNode, ifsNode, ifsCondNode.name + "_" + ifsNode.name, null, null)) :::
        List[DfgEdge[DfgNode]](DfgEdge[DfgNode](ifsLgcGrphEndNode, ifsCondNode, ifsLgcGrphEndNode.name + "_" + ifsCondNode.name, null, null))

      val ifsCodeBlockList = parseExp(ifsNode.level, Some(ifs))

      (fn) match {
        case (ifn: IfNode) => list = list ::: ifsCodeBlockList ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode](ifsNode, ifn
          .asInstanceOf[IfNode], ifsNode.name + "_" + ifn.asInstanceOf[IfNode].name, null, null))
        case (elfn: ElsIfNode) => list = list ::: ifsCodeBlockList ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode](ifsNode, elfn
          .asInstanceOf[ElsIfNode], ifsNode.name + "_" + elfn.asInstanceOf[ElsIfNode].name, null, null))
        case (elsn: ElsNode) => list = list ::: ifsCodeBlockList ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode](ifsNode, elsn
          .asInstanceOf[ElsNode], ifsNode.name + "_" + elsn.asInstanceOf[ElsNode].name, null, null))
        case (stn: StateNode) => list = list ::: ifsCodeBlockList ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode](ifsNode, stn
          .asInstanceOf[StateNode], ifsNode.name + "_" + stn.asInstanceOf[StateNode].name, null, null))
        case _ =>
      }
      list
    }

    def model(elems: Seq[Any]): Graph[DfgNode, DfgEdge] = {
      var g = Graph.empty[DfgNode, DfgEdge]
      var nl: List[DfgNode] = List()
      var cl: List[DfgEdge[DfgNode]] = List()
      val elemList = elems.toList.distinct

      elemList.foreach { s =>
        (s) match {
          case (comb: Combinational) => g = g ++ parseExp(0, Some(comb))
          case (seq: Sequential) => g = g ++ parseExp(0, Some(seq))
          case (stm: Streamer) if stm.lvl == 0 => g = g + StrmNode(stm)
          case (stm: Streamer) if stm.lvl > 0 => g = g ++ parseExp(stm.lvl, stm.logicExp)
          case (arith2: Arith2Arg) => g = g ++ parseExp(0, Some(arith2))
          case (logi2: Logi2Arg) => g = g ++ parseExp(0, Some(logi2))
          case (fsm: Fsm) => g = g ++ parseExp(0, Some(fsm))
          case (ifs: If) => g = g ++ parseExp(0, Some(ifs))
          case (comp: Component) =>{

            var list: List[DfgEdge[DfgNode]] = List()
            val cNode = if (comp.dfg.isEmpty) TmplNode(comp) else CompNode(comp)

            comp._inlinks.foreach { lnk =>
              list = list ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode]
                (lnk._1, cNode, lnk._1.inst  + "_" + lnk._2.name + "_" + cNode.inst + "_" + lnk._4.name, lnk._2, lnk._4))
            }

            comp._outlinks.foreach { lnk =>
              list = list ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode]
                (cNode, lnk._3, cNode.inst  + "_" + lnk._2.name + "_"  + lnk._1.inst + "_" + lnk._4.name,  lnk._2, lnk._4))
            }
            g = g ++ list
          }
          //case _ => null
        }
      }
      //g.draw(inst)
      g
    }
  }

  case class Comp(inst: String, name: String, override val dfg: Graph[DfgNode, DfgEdge]) extends Component

  def :=(xs: Any*): Seq[Any] = xs.toList

  case class ConstVal(inst: String, vl: Int, override val width: Int) extends Component {
    override val name = "ConstVal"
    override val dfg = model(Seq())
    _params = Map("CONSTVALUE" -> vl)
  }

  case class Constants(width: Int)(vls: Int*)
  {
    def comps: Seq[ConstVal] = vls.map( vl => new ConstVal(constName(vl), vl, width))
  }
  object  Constants{
    //def apply(width: Int)(vls: Int*): Constants = new Constants(width, vls)
  }
  //implicit def addConstVals(consts: Constants): Seq[ConstVal] = consts.vls.map( vl => new ConstVal(constName(vl), vl, consts.width))
  implicit def addConstVal(vl: Int)(implicit width: Int): ConstVal = new ConstVal(constName(vl), vl, width)
  //def Constants()

  def constName(vl: Int) = {
    if (vl < 0)
      "ct_" + randomStr(6) + "_Neg" + vl.abs
    else
      "ct_" + randomStr(6) + "_" + vl.abs
  }

  case class DummyComp(inst: String, n: Node) extends Component {
    //override val inst = ins
    override val name = "DummyComp"
    override val width: Int = n.width
    override val dfg = Graph[DfgNode, DfgEdge](n)
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Directives
  //--------------------------------------------------------------------------------------------------------------------
  abstract class Directive extends Exp
  case class Combinational(cb: Any*) extends Directive {
    val _name = randomStr(6)
    var _codeBlock = cb
  }

  case class Sequential(cb: Any*) extends Directive {
    val _name = randomStr(6)
    var _codeBlock = cb
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Parameterizable Templates
  //--------------------------------------------------------------------------------------------------------------------
  case class Delay(inst: String, w: Int, depth: Int) extends Component {
    val en = Streamer("en", 1, PortTypeEnum.EN)
    val din = Streamer("din", w, PortTypeEnum.DIN)
    val vld = Streamer("vld", 1, PortTypeEnum.VLD)
    val dout = Streamer("dout", w, PortTypeEnum.DOUT)

    override val name = "delay"
    override val dfg = model(Seq())
    override val width = w
    _params = Map("width" -> w, "depth" -> depth)
    _inports = List(en, din)
    _outports = List(dout, vld)
  }

  case class Mux2to1(inst: String, w: Int) extends Component {
    val sel = Streamer("sel", 1, PortTypeEnum.EN)
    val din1 = Streamer("din1", w, PortTypeEnum.DIN)
    val din2 = Streamer("din2", w, PortTypeEnum.DIN)
    val dout = Streamer("dout", w, PortTypeEnum.DOUT)

    override val name = "mux2to1"
    override val dfg = model(Seq())
    override val width = w
    _params = Map("width" -> w)
     _inports = List(sel, din1, din2)
     _outports = List(dout)
  }

  case class Counter(inst: String, w: Int) extends Component {
    val en = Streamer("en", 1, PortTypeEnum.EN)
    val dout = Streamer("dout", w, PortTypeEnum.DOUT)

    override val name = "counter"
    override val dfg = model(Seq())
    override val width = w
    _params = Map("width" -> w,  "inst" -> inst)
    _inports = List(en)
    _outports = List(dout)
  }

  case class Rom(inst: String, data_width: Int, vector: Seq[Int]) extends Component {
    val addr_width = Math.ceil(Math.log10(vector.length) / Math.log10(2)).toInt
    val addr = Streamer("addr", addr_width, PortTypeEnum.DIN)
    val dout = Streamer("dout", data_width, PortTypeEnum.DOUT)

    override val name = "rom"
    override val dfg = model(Seq())
    override val width = data_width
    _params = Map("data_width" -> data_width, "addr_width" -> addr_width, "vector" -> vector,  "inst" -> inst)
    _inports = List(addr)
    _outports = List(dout)
  }

  case class Rounder(inst: String, din_width: Int, dout_width: Int) extends Component {
    val din = Streamer("din", din_width, PortTypeEnum.DIN)
    val dout = Streamer("dout", dout_width, PortTypeEnum.DOUT)

    override val name = "rounder"
    override val dfg = model(Seq())
    override val width = dout_width
    _params = Map("din_width" -> din_width, "dout_width" -> dout_width, "inst" -> inst)
    _inports = List(din)
    _outports = List(dout)
  }

  /// General functions
  def getArith2ArgNodeWidth(prfx: String, argWidth1: Int, argWidth2: Int): Int = prfx match {
    case "add" => List(argWidth1, argWidth2).max
    case "sub" => List(argWidth1, argWidth2).max
    case "mul" => argWidth1 + argWidth2
    case "div" => List(argWidth1, argWidth2).max
    case "le" => List(argWidth1, argWidth2).max
    case "and" => List(argWidth1, argWidth2).max
    case "or" => List(argWidth1, argWidth2).max
    case _ => 0
  }
}
