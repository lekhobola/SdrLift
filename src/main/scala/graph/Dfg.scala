package sdrlift.graph


import scala.util.control.Breaks

import scalax.collection.GraphPredef._
import scalax.collection.Graph
import scalax.collection.GraphTraversal.DepthFirst
import scalax.collection.io.dot._
import Indent._
import implicits._

import scala.collection.mutable.ArraySeq
import java.io.PrintWriter

import de.upb.hni.vmagic.`object`.VhdlObject.Mode
import de.upb.hni.vmagic.libraryunit.Entity
import de.upb.hni.vmagic.util.VhdlCollections
import exp.CompExp.ConstVal
import scalax.collection.GraphTraversal.{DepthFirst, Predecessors}
import sdrlift.graph.NodeFactory._

import scala.annotation.tailrec
import sys.process._
import scala.language.postfixOps
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer


object Dfg {

  implicit class DirectedFlowGraph[N, E[X] <: EdgeLikeIn[X]](g: Graph[DfgNode, DfgEdge]) {

    def getNode(n: g.NodeT): Node = n.value match {
      //Node Factory
      case combNode: DirNode => combNode
      case strmNode: StrmNode => strmNode
      case cnst: ConstNode => cnst
      case arithNode: ArithNode => arithNode
      case lgcNode: LogiNode => lgcNode
      case condNode: CondNode => condNode
      case fsmNode: FsmNode => fsmNode
      case stNode: StateNode => stNode
      case ifNode: IfNode => ifNode
      case eifNode: ElsIfNode => eifNode
      case elsNode: ElsNode => elsNode
      case cmpNode: CompNode => cmpNode
      case tmplNode: TmplNode => tmplNode
      //case krnlNode: KernelNode => krnlNode
      case _ => null
    }


    /* def cycleContaining(node: DfgNode)= {
      g.findCycleContaining(g get node)
    } */

    def findAllPathFrom[N, E[X] <: EdgeLikeIn[X]](graph:Graph[N,E])(node:graph.NodeT):Seq[graph.Path] = {
      @tailrec
      def findAllPathFrom(node: graph.NodeT, previousFoundPath: Seq[graph.Path]): Seq[graph.Path] = {
        val newPath = node pathUntil (node => !previousFoundPath.exists(_.endNode == node))
        newPath match {
          case Some(path) => findAllPathFrom(node, previousFoundPath :+ path)
          case None => previousFoundPath
        }
      }

      findAllPathFrom(node, Seq.empty[graph.Path])
    }



    def getOrderedNodes: List[Node] = {
      val roots = getRootNodes
      var iSeq = IndexedSeq[(Int, g.NodeT)]()
      for (r <- roots) {
        val xs = r.innerNodeTraverser.map(n => n).toSeq
        val seq = (xs.indices zip xs)
        iSeq = iSeq ++ seq
      }

      val list = iSeq.toList.sortBy(_._1).distinct.map(_._2)
      list.map { n => getNode(n) }
    }


    private def getRootNodes: List[g.NodeT] = (g nodes).filter(p => p.incoming.isEmpty).toList

    def getRoots: List[Node] = getRootNodes.map { n => getNode(n) }.toList

    private def getLeafNodes: List[g.NodeT] = {
      (g nodes).filter(p => p.outgoing.isEmpty).toList
    }

    def getLeaves: List[Node] = getLeafNodes.map { n => getNode(n.asInstanceOf[g.NodeT]) }.toList

    private def getBranchNodes: List[g.NodeT] = (g nodes).filterNot((getRootNodes ::: getLeafNodes).contains(_)).toList

    def getBranches: List[Node] = {
      val roots = (g nodes).filter(p => p.incoming.isEmpty).toList
      val leaves = (g nodes).filter(p => p.outgoing.isEmpty).toList
      val outerNodes = roots ::: leaves
      val ns = (g nodes).filterNot(outerNodes.contains(_)).toList

      ns.map { n => getNode(n.asInstanceOf[g.NodeT]) }.toList
    }

    def getElList(ns: List[g.NodeT]): List[DfgNode] = ns.map { n => getNode(n) }.toList

    def getPredecessorNodes(n: DfgNode, kl: List[String]): List[DfgNode] = {
      def getPreds(n: DfgNode, predList: List[DfgNode]): List[DfgNode] = {
        var pn = ((g get n).diPredecessors).find(e => kl.contains(e.prefix))
        pn match {
          case Some(x) => List(getNode(x)) ::: getPreds(x, predList)
          case None => predList
        }
      }

      var predList: List[DfgNode] = List()
      getPreds(n, predList)
    }

    /*
    def getCodeBlocks(el: DfgNode, kl: List[String]): List[Node] = {
      var predList: List[Node] = List()
      var nonAssignPrfxs = List("if") //Prefixes of Sub-Nodes with potential assignments children nodes
      // filter out the di-predecessors speciefied in kl
      val predNodes = ((g get el).diPredecessors).filterNot(n => kl.contains(n.prefix))

      // filter out the sub-nodes with assignment children nodes
      // val predFiltNodes = predNodes.filterNot(n => nonAssignPrfxs.contains(n.prefix))
      val predFiltNodes = ((g get el).diPredecessors).filter(n => nonAssignPrfxs.contains(n.prefix))
      // select the sub-nodes with assignment children nodes
      val predSelNodes = predNodes.filter(n => nonAssignPrfxs.contains(n.prefix))

      for (i <- predFiltNodes) {
        val travList = (g get i).innerNodeTraverser.withDirection(Predecessors).filterNot(_ match {
          case g.InnerNode(n) => kl.contains(n.prefix)
        })
        predList = predList ::: travList.map(n => getNode(n)).toList
      }

      predList = predList ::: predSelNodes.map(n => getNode(n)).toList

      predList.reverse
    }
     */

    def getCodeBlocksFiltNot(el: DfgNode, kl: List[String]): List[Node] = {
      var predList: List[Node] = List()
      var nonAssignPrfxs = List("if") //Prefixes of Sub-Nodes with potential assignments children nodes
      // filter out the di-predecessors speciefied in kl
      val predNodes = ((g get el).diPredecessors)

      for (i <- predNodes) {
        val travList = (g get i).innerNodeTraverser.withDirection(Predecessors).filterNot(_ match {
          case g.InnerNode(n) => kl.contains(n.prefix)
        })
        predList = predList ::: travList.map(n => getNode(n)).toList
      }

      predList = predList ::: predNodes.map(n => getNode(n)).toList

      predList.reverse.filterNot(n => (nonAssignPrfxs ::: kl).contains(n.prefix))
    }

    def getDiPredNodes(n: DfgNode, kl: List[String] = List()): List[Node] = {
      var predList: List[DfgNode] = List()
      var pl: List[g.NodeT] = List()
      if (kl.isEmpty)
        pl = ((g get n).diPredecessors).toList
      else
        pl = ((g get n).diPredecessors).toList.filter(el => kl.contains(el.prefix))

      for (i <- pl) {
        predList = predList ::: List(getNode(i))
      }

      predList.asInstanceOf[List[Node]]
    }

    def getDiPredEdges(n: DfgNode) = {
      (g get n).incoming.map(e => (getNode(e.source), e.srcPort, getNode(e.target), e.snkPort)).toList
    }

    // Get the Conditional-Logical (CL) Nodes
    def getDiCLNodes(n: DfgNode): List[LogiNode] = {
      val kl = List("cnd", "lgc")
      var predList: List[DfgNode] = List()
      val cn = ((g get n).diPredecessors).toList.find { el => kl.contains(el.prefix) }
      if (cn != None) {
        var pl = ((g get cn.get).diPredecessors).toList.filter(el => getNode(el).isInstanceOf[LogiNode])

        for (i <- pl) {
          predList = predList ::: List(getNode(i))
        }
      }
      predList.asInstanceOf[List[LogiNode]]
    }

    def getGraphEndNode: DfgNode = { // List[g.NodeT]
      val endNodes = (g nodes).filter(p => p.outgoing.isEmpty).toList
      g get endNodes.head
      //getNode(endNodes.get(0))
    }

    def getCondSubPredList(condNode: Node): List[Node] = {
      val lgcNode = getDiPredNodes(condNode, List("lgc")).head
      val preds = (g get lgcNode).innerNodeDownUpTraverser.withKind(DepthFirst).withDirection(Predecessors).filter { el =>
        el._1 == false
      }.map(_._2).toList.filter(n => getNode(n).isInstanceOf[LogiNode]).toList

      preds.map(el => getNode(el)).toList
    }

    private def getPathsFromRootsToLeaves: List[g.Path] = {
      var paths: List[g.Path] = List()

      val leafNode = getLeafNodes.head
      val rootNodes = getRootNodes
      val diPredNodes = (g get getNode(leafNode)).diPredecessors.toList

      rootNodes.foreach { r =>
        diPredNodes.foreach { d =>
          val path = r pathTo d
          val tmpList = if (path != None) List(path.get.asInstanceOf[g.Path]) else List()
          paths = paths ::: tmpList
        }
      }
      paths
    }

    def getArithLeafNodes: List[Node] = {
      val paths = getPathsFromRootsToLeaves
      paths.map { p =>
        p.nodes.filter(n => getNode(n).isInstanceOf[ArithNode]).lastOption
      }.toList.flatten.map(n => getNode(n)).distinct
    }

    def draw(name: String): Unit = {

      val root = new DotRootGraph(directed = true, id = Some("Dot"))

      def edgeTransformer(innerEdge: Graph[DfgNode, DfgEdge]#EdgeT): Option[(DotGraph, DotEdgeStmt)] = {
        val edge = innerEdge.edge
        val label = edge.id //"[" + edge.srcPort.rate + "," + edge.snkPort.rate + "," + edge.dly + "]"
        //val fromNode = edge.from.nodeName
        Some((root,
          DotEdgeStmt(edge.source.toString,//value.inst,
            edge.target.toString,//.value.inst,
            if (label.nonEmpty) List(DotAttr("label", label.toString))
            else Nil)))
      }

      val dot = g.toDot(root, edgeTransformer(_))
      val dotFile = new PrintWriter("out/dot/" + name + ".dot")
      dotFile.println(dot.toString)
      dotFile.close
      "dot -Tpng out/dot/" + name + ".dot -o out/dot/" + name + ".png" !
    }

    def schematic(name: String): Unit = {
      val root = new DotRootGraph(directed = true, id = None, attrStmts = List(DotAttrStmt(Elem.graph, List(DotAttr("rankdir", "LR"), DotAttr("fontname", "Arial"), DotAttr("margin", "0"), DotAttr("dpi", "600")))))

      def edgeTransformer(innerEdge: Graph[DfgNode, DfgEdge]#EdgeT): Option[(DotGraph, DotEdgeStmt)] = {
        val edge = innerEdge.edge
        val label = edge.id

        val from = if (edge.srcPort != null && edge.source.value.isInstanceOf[CompNode]) {
          if (!edge.source.value.asInstanceOf[CompNode].comp.isInstanceOf[ConstVal])
            edge.source.value.asInstanceOf[CompNode].inst + ":" + (if (edge.srcPort.index > -1) edge.srcPort.name + "_" + edge.srcPort.index else edge.srcPort.name)
          else
            edge.source.value.asInstanceOf[CompNode].inst
        } else if (edge.srcPort != null && edge.source.value.isInstanceOf[TmplNode]) {
          if (!edge.source.value.asInstanceOf[TmplNode].comp.isInstanceOf[ConstVal])
            edge.source.value.asInstanceOf[TmplNode].inst + ":" + (if (edge.srcPort.index > -1) edge.srcPort.name + "_" + edge.srcPort.index else edge.srcPort.name)
          else
            edge.source.value.asInstanceOf[TmplNode].inst
        }
        else edge.source.value.inst

        val to = if (edge.snkPort != null && edge.target.value.isInstanceOf[CompNode]) {
          if (!edge.target.value.asInstanceOf[CompNode].comp.isInstanceOf[ConstVal])
            edge.target.value.asInstanceOf[CompNode].inst + ":" + ((if (edge.snkPort.index > -1) edge.snkPort.name + "_" + edge.snkPort.index else edge.snkPort.name))
          else
            edge.target.value.asInstanceOf[CompNode].inst
        } else if (edge.snkPort != null && edge.target.value.isInstanceOf[TmplNode]) {
          if (!edge.target.value.asInstanceOf[TmplNode].comp.isInstanceOf[ConstVal])
            edge.target.value.asInstanceOf[TmplNode].inst + ":" + ((if (edge.snkPort.index > -1) edge.snkPort.name + "_" + edge.snkPort.index else edge.snkPort.name))
          else
            edge.target.value.asInstanceOf[TmplNode].inst
        }
        else edge.target.value.inst

        Some((root, DotEdgeStmt(from, to)))
      }

      def myNodeTransformer(innerNode: Graph[DfgNode, DfgEdge]#NodeT): Option[(DotGraph, DotNodeStmt)] = {
        val node = innerNode.value
        Some((root,
          DotNodeStmt(node.value.inst,
            getShapeAttrs(node.value))))

      }

      val dot = g.toDot(dotRoot = root,
        edgeTransformer = edgeTransformer,
        cNodeTransformer = Some(myNodeTransformer),
        spacing = DefaultSpacing)
      val dotLines = dot.split("\n").toSeq
      //.map(_.trim).filter(_ != "")
      val formattedDot = dotLines.map { l =>
        if (l.contains(":")) l.replace("\"", "") else l
      }.toList.mkString("\n")

      val dotFile = new PrintWriter("out/dot/" + name + ".dot")
      dotFile.println(formattedDot)
      dotFile.close
      //"dot -Tpng out/dot/" + name + ".dot -o out/dot/" + name + ".png" !
      "dot -Tpdf out/dot/" + name + ".dot -o out/dot/" + name + ".pdf" !
    }

    def drawIR(name: String): Unit = {
      val root = new DotRootGraph(directed = true, id = None, attrStmts = List(DotAttrStmt(Elem.graph, List(DotAttr("fontname", "Arial"), DotAttr("margin", "0"), DotAttr("dpi", "600")))))

      def edgeTransformer(innerEdge: Graph[DfgNode, DfgEdge]#EdgeT): Option[(DotGraph, DotEdgeStmt)] = {
        val edge = innerEdge.edge
        val label = edge.id

        val from =  edge.source.value.inst
        val to = edge.target.value.inst

        Some((root, DotEdgeStmt(from, to)))
      }

      def nodeTransformer(innerNode: Graph[DfgNode, DfgEdge]#NodeT): Option[(DotGraph, DotNodeStmt)] = {
        val node = innerNode.value
        Some((root, DotNodeStmt(node.value.inst, getIRShapeAttrs(node.value))))
      }

      val dot = g.toDot(dotRoot = root,
        edgeTransformer = edgeTransformer,
        cNodeTransformer = Some(nodeTransformer),
        spacing = DefaultSpacing)

      val dotFile = new PrintWriter("out/dot/" + name + ".dot")
      dotFile.println(dot)
      dotFile.close
      // "dot -Tpng out/dot/" + name + ".dot -o out/dot/" + name + ".png" !
      "dot -Tpdf out/dot/" + name + ".dot -o out/dot/" + name + ".pdf" !
    }

    def getShapeAttrs(n: DfgNode): ListBuffer[DotAttr] = {

      var attrList = ListBuffer[DotAttr]()
      attrList = attrList += DotAttr("label", getNodeId(n))
      attrList = attrList += DotAttr("fontname", "Arial")
      n match {
        case arithNode: ArithNode => {
          attrList = attrList += DotAttr("shape", "circle")
          attrList = attrList += DotAttr("width", "0")
          attrList = attrList += DotAttr("height", "0")
          attrList = attrList += DotAttr("margin", "0")
          attrList = attrList += DotAttr("style", "filled")
          attrList = attrList += DotAttr("fillcolor", "bisque2")
          attrList = attrList += DotAttr("color", "bisque4")
        }
        case logiNode: LogiNode => {
          attrList = attrList += DotAttr("shape", "square")
          attrList = attrList += DotAttr("width", "0")
          attrList = attrList += DotAttr("height", "0")
          attrList = attrList += DotAttr("margin", "0")
          attrList = attrList += DotAttr("style", "filled")
          attrList = attrList += DotAttr("fillcolor", "bisque2")
          attrList = attrList += DotAttr("color", "bisque4")
        }
        case strmNode: StrmNode => {
          attrList = attrList += DotAttr("shape", "Msquare")
          attrList = attrList += DotAttr("width", "0")
          attrList = attrList += DotAttr("height", "0")
          attrList = attrList += DotAttr("margin", "0.03")
        }
        case dirNode: DirNode => {
          attrList = attrList += DotAttr("shape", "record")
        }
        case compNode: CompNode => {
          attrList = attrList += DotAttr("width", "0")
          attrList = attrList += DotAttr("height", "0")
          attrList = attrList += DotAttr("shape", "Mrecord")
          if (!compNode.comp.isInstanceOf[ConstVal]) {
            attrList = attrList += DotAttr("style", "filled")
            attrList = attrList += DotAttr("fillcolor", "lightskyblue2")
            attrList = attrList += DotAttr("color", "lightskyblue4")
          } else {
            attrList = attrList += DotAttr("style", "filled")
            attrList = attrList += DotAttr("fillcolor", "goldenrod2")
            attrList = attrList += DotAttr("color", "goldenrod4")
          }
        }
        case tmplNode: TmplNode => {
          attrList = attrList += DotAttr("width", "0")
          attrList = attrList += DotAttr("height", "0")
          attrList = attrList += DotAttr("shape", "Mrecord")
          if (!tmplNode.comp.isInstanceOf[ConstVal]) {
            attrList = attrList += DotAttr("style", "filled")
            attrList = attrList += DotAttr("fillcolor", "darkolivegreen2")
            attrList = attrList += DotAttr("color", "darkolivegreen4")
          }else{
            attrList = attrList += DotAttr("style", "filled")
            attrList = attrList += DotAttr("fillcolor", "goldenrod2")
            attrList = attrList += DotAttr("color", "goldenrod4")
          }
        }
        case _ => {
          attrList = attrList += DotAttr("shape", "rectangle")
        }
      }
      attrList
    }

    def getNodeId(n: DfgNode): String = {
      n match {
        case arithNode: ArithNode => {
          arithNode.opr match {
            case "add" => "<&#43;>"
            case "sub" => "<&#8722;>"
            case "mul" => "<&#215;>"
            case "div" => "<&#247;>"
          }
        }
        case logiNode: LogiNode => {
          logiNode.opr match {
            case "le" => "<="
            case "and" => "&&"
            case "or" => "||"
            case "not" => "<&#172;>"
          }
        }
        case dirNode: DirNode => {
          // "{Directive|" + n.inst + "}"
          "Directive"
        }
        case compNode: CompNode => {
          if (!compNode.comp.isInstanceOf[ConstVal]) {
            val inPortsIds = (g get compNode).incoming.map(e => if (e.snkPort.index > -1) e.snkPort.name + "_" + e.snkPort.index else e.snkPort.name).toList

            val outPortsIds = (g get compNode).outgoing.map(e => if (e.srcPort.index > -1) e.srcPort.name + "_" + e.srcPort.index else e.srcPort.name).toList
            var inLabel = ""
            var i = 0
            for (i <- 0 until inPortsIds.length) {
              val p = inPortsIds(i)
              if (i == 0) inLabel = inLabel + "{"
              if (i < inPortsIds.length - 1) inLabel = inLabel + "<" + p + ">" + p + "|" else inLabel = inLabel + "<" + p + ">" + p + "}|"
            }
            var outPorts = ""
            i = 0
            for (i <- 0 until outPortsIds.length) {
              val p = outPortsIds(i)
              if (i == 0) outPorts = outPorts + "|{"
              if (i < outPortsIds.length - 1) outPorts = outPorts + "<" + p + ">" + p + "|" else outPorts = outPorts + "<" + p + ">" + p + "}"
            }
            compNode.inst + "|{ " + inLabel + outPorts + " }"
          } else {
            compNode.comp.asInstanceOf[ConstVal].vl.toString
          }
        }
        case tmplNode: TmplNode => {
          if(!tmplNode.comp.isInstanceOf[ConstVal]) {
            val inPortsIds = (g get tmplNode).incoming.map(e => if (e.snkPort.index > -1) e.snkPort.name + "_" + e.snkPort.index else e.snkPort.name).toList

            val outPortsIds = (g get tmplNode).outgoing.map(e => if (e.srcPort.index > -1) e.srcPort.name + "_" + e.srcPort.index else e.srcPort.name).toList
            var inLabel = ""
            var i = 0
            for (i <- 0 until inPortsIds.length) {
              val p = inPortsIds(i)
              if (i == 0) inLabel = inLabel + "{"
              if (i < inPortsIds.length - 1) inLabel = inLabel + "<" + p + ">" + p + "|" else inLabel = inLabel + "<" + p + ">" + p + "}|"
            }
            var outPorts = ""
            i = 0
            for (i <- 0 until outPortsIds.length) {
              val p = outPortsIds(i)
              if (i == 0) outPorts = outPorts + "|{"
              if (i < outPortsIds.length - 1) outPorts = outPorts + "<" + p + ">" + p + "|" else outPorts = outPorts + "<" + p + ">" + p + "}"
            }
            tmplNode.inst + "|{ " + inLabel + outPorts + " }"
          }else{
            tmplNode.comp.asInstanceOf[ConstVal].vl.toString
          }
        }
        case _ => {
          n.inst
        }
      }
    }

    def getIRShapeAttrs(n: DfgNode): ListBuffer[DotAttr] = {

      var attrList = ListBuffer[DotAttr]()
      attrList = attrList += DotAttr("label", getIRNodeId(n))
      attrList = attrList += DotAttr("fontname", "Arial")

      attrList = attrList += DotAttr("shape", "Mrecord")
      //attrList = attrList += DotAttr("width", "0")
     // attrList = attrList += DotAttr("height", "0")
      //attrList = attrList += DotAttr("margin", "0")
     // attrList = attrList += DotAttr("style", "filled")
     // attrList = attrList += DotAttr("fillcolor", "bisque2")
     // attrList = attrList += DotAttr("color", "bisque4")
      attrList
    }

    def getIRNodeId(n: DfgNode): String = {
      n match {
        case strmNode: StrmNode => {
          val inStms = getRootNodes
          val outStms = getLeafNodes
          val portTypId = if (inStms.contains(strmNode)) "input"
          else if (outStms.contains(strmNode)) "output"
          else "none"

          "{{StrmNode}|" +
            "id = " + strmNode.inst +
            "\\ntype = " + portTypId +
            "\\nwidth = " + strmNode.width + "}"
        }
        case constNode: ConstNode => {
          "{{ConstNode}|" +
            "id = " + constNode.inst +
            "\\nvalue = " + constNode.vl +
            "\\nwidth = " + constNode.width + "}"
        }
        case arithNode: ArithNode => {
          "{{ArithNode}|" +
            "id = " + arithNode.inst +
            "\\nop = " + arithNode.opr +
            "\\nwidth = " + arithNode.width + "}"
        }
        case logiNode: LogiNode => {
          "{{LogiNode}|" +
            "id = " + logiNode.inst +
            "\\nop = " + logiNode.opr +
            "\\nwidth = " + logiNode.width + "}"
        }
        case cmpNode: CompNode => {
          val hdlfile = cmpNode.vhdlfile
          val hdlEntity = VhdlCollections.getAll(hdlfile.getElements, classOf[Entity]).head
          val portGenerics = hdlEntity.getGeneric.toList.distinct.map(_.getVhdlObjects.get(0))
          val portSignals = hdlEntity.getPort.toList.distinct.map(_.getVhdlObjects.get(0))
          "{{CompNode}|" +
            "id = " + cmpNode.inst +
            "\\ngenerics # = " + portSignals.count(_.getMode == Mode.IN) +
            "\\ninputs # = " + 0 +
            "\\noutputs # = " + portSignals.count(_.getMode == Mode.OUT)  + "}"
        }
        case tmplNode: TmplNode => {
          if(!tmplNode.comp.isInstanceOf[ConstVal]) {
            val hdlfile = tmplNode.vhdlfile
            val hdlEntity = VhdlCollections.getAll(hdlfile.getElements, classOf[Entity]).head
            val portGenerics = hdlEntity.getGeneric.toList.distinct.map(_.getVhdlObjects.get(0))
            val portSignals = hdlEntity.getPort.toList.distinct.map(_.getVhdlObjects.get(0))
            "{{TmplNode}|" +
              "id = " + tmplNode.inst +
              "\\ngenerics # = " + portGenerics.length +
              "\\ninputs # = " + portSignals.count(_.getMode == Mode.IN) +
              "\\noutputs # = " + portSignals.count(_.getMode == Mode.OUT)  + "}"
          }else{
            val constNode = tmplNode.comp.asInstanceOf[ConstVal]
            "{{ConstNode}|" +
              "id = " + constNode.inst +
              "\\nvalue = " + constNode.vl +
              "\\nwidth = " + constNode.width + "}"
          }
        }
        case dirNode: DirNode => {
          "{{DirNode}|" +
            "type = " + dirNode.ctrl + "}"
        }
        case _ => {
          n.inst
        }
      }
    }
  }

  implicit class ExtGraphNode[N, E[X] <: EdgeLikeIn[X]](node_ : Graph[DfgNode, DfgEdge]#NodeT) {
    type NodeT = graph.NodeT
    val graph = node_.containingGraph
    val node = node_.asInstanceOf[NodeT]
    val graphSize = graph.graphSize
    val order = graph.order
    val size = graph.size
  }

  implicit class GraphEnrichment[N, E[X] <: EdgeLikeIn[X]](val g: Graph[N, E]) extends AnyVal {

  }

}
