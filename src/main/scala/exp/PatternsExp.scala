package exp

import scala.collection.JavaConversions._
import scalax.collection.Graph
import sdrlift.graph.Dfg._
import sdrlift.graph.{DfgEdge, DfgNode}
import sdrlift.graph.NodeFactory._
import NodeExp._
import exp.CompExp._
import exp.KernelExp.{Kernel, KernelPort}
import sdrlift.model.{Actor, Channel}

import scala.collection.generic.{CanBuildFrom, GenericTraversableTemplate, SeqFactory}
import scala.collection.{Iterator, Seq, SeqLike}
import scala.collection.immutable.Map
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

object PatternsExp {
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /// Toplogical Patterns
  ///////////////////////
  object Chain extends SeqFactory[Chain] {
    implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, Chain[A]] =
      new GenericCanBuildFrom[A]

    def newBuilder[A] = new ListBuffer[A] mapResult (x => new Chain(x: _*))
  }

  class Chain[A](seq: A*)
    extends Seq[A]
      with GenericTraversableTemplate[A, Chain]
      with SeqLike[A, Chain[A]] {

    override def companion = Chain

    def iterator: Iterator[A] = seq.iterator

    def apply(idx: Int): A = {
      if (idx < 0 || idx >= length) throw new IndexOutOfBoundsException
      seq(idx)
    }

    def length: Int = seq.size

    def dfg: Graph[DfgNode, DfgEdge] = {

      def loop(seq: Seq[Component], grph: Graph[DfgNode, DfgEdge]): Graph[DfgNode, DfgEdge] = seq.toList match {
        case Nil => grph //empty list
        case el :: Nil => grph // last element
        case el :: t => { // complete list
          var edgeList = List.empty[DfgEdge[DfgNode]]
          var srcNode: DfgNode = null
          var tgtNode: DfgNode = null

          if (el._params == null) {
            srcNode = CompNode(el)
            tgtNode = CompNode(t.head)
          } else {
            srcNode = TmplNode(el)
            tgtNode = TmplNode(t.head)
          }

          el._outlinks.foreach { lnk =>
            edgeList = edgeList ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode]
              (srcNode, tgtNode, srcNode.inst + "_" + lnk._1.inst + "_" + tgtNode.inst + "_" + lnk._3.inst, lnk._2, lnk._4))
          }
          loop(t, grph ++ edgeList)
        }
      }

      val grph = loop(seq.asInstanceOf[Seq[Component]], Graph.empty[DfgNode, DfgEdge])
      grph.draw("Chain")
      grph
    }

    def comps = dfg.getOrderedNodes.map { el =>
      el match {
        case compNode: CompNode => compNode.comp
        case tmplNode: TmplNode => tmplNode.comp
      }
    }

    def sdfap: Graph[Actor, Channel] = {

      def actorParams(pArray: Map[String, Any]) = {
        val prms = new java.util.HashMap[String, Any]()
        pArray.foreach { case (k,v) => prms.put(k, v) }
        prms
      }

      def loop(seq: Seq[Kernel], grph: Graph[Actor, Channel]): Graph[Actor, Channel] = seq.toList match {
        case Nil => grph //empty list
        case el :: Nil => grph // last element
        case el :: t => { // complete list
          var edgeList = List.empty[Channel[Actor]]

          // generic parameters
          val srcParams = el._params
          val tgtParams = t.head._params

          // create an access patterns as an array of binary values
          val srcPort = el._outports.find(_.typ == PortTypeEnum.DOUT).head
          val tgtPort = t.head._inports.find(_.typ == PortTypeEnum.DIN).head
          val srcAp = (((srcPort.ap flatMap { e => List.fill(e._1)(e._2) }).toList).map { e => e.toList }.flatten).
            map (_.asDigit)
          val tgtAp = (((tgtPort.ap flatMap { e => List.fill(e._1)(e._2) }).toList).map { e => e.toList }.flatten).
            map (_.asDigit)

          val srcNode = Actor(el.name, el.inst, actorParams(srcParams), srcAp.length)
          val tgtNode = Actor(t.head.name, t.head.inst, actorParams(tgtParams), tgtAp.length)

          val srcRate = srcAp.count(_ == 1)
          val tgtRate = tgtAp.count(_ == 1)

          if (el._outlinks.size > 0) {
            for (i <- 0 until el._outlinks.size) {
              val lnk = el._outlinks(i)
              val outPortsLabels = el._outports.filter(_.typ == PortTypeEnum.DOUT)
              val outPrms = new java.util.HashMap[String, String]()
              outPrms.put("dout", outPortsLabels.map(_.name).mkString(","))
              val inPortsLabels = t.head._inports.filter(_.typ == PortTypeEnum.DIN)
              val inPrms = new java.util.HashMap[String, String]()
              inPrms.put("dout", inPortsLabels.map(_.name).mkString(","))

              val ch = Channel[Actor](srcNode, tgtNode, "ch_" + srcNode.inst + lnk._2.name + "_" + tgtNode
                .inst + lnk._4.name, KernelPort(srcRate, srcPort.ap, outPrms),
                KernelPort(tgtRate, tgtPort.ap, inPrms), 0, Tuple2(srcNode.inst + lnk._2.name, tgtNode.inst
                  + lnk._4.name))

              edgeList = edgeList ::: List(ch)
            }
          } else {
            val ch = Channel[Actor](srcNode, tgtNode, "ch_" + srcNode.inst + "_" + tgtNode.inst,
              KernelPort(srcRate, srcPort.ap, null),
              KernelPort(tgtRate, tgtPort.ap, null), 0, null)
            edgeList = edgeList ::: List(ch)
          }

          loop(t, grph ++ edgeList)
        }
      }

      val grph = loop(seq.asInstanceOf[Seq[Kernel]], Graph.empty[Actor, Channel])
      grph
    }
  }


  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /// Data Patterns
  ///////////////////////
  abstract class DataPattern

  case class FoldR(dfg: Graph[DfgNode, DfgEdge]) extends DataPattern {
    def out =  Streamer("dummyexp", 1, 0, Some(getNodeExp(dfg.getLeaves.head)))
  }

  object FoldR {

    def buildFoldR(seq: Seq[Component], f: (Component, Component) => Component): FoldR={
      var g = Graph.empty[DfgNode, DfgEdge]
      if (seq.size > 1) {
        var tmp = f(seq(0), seq(1))
        g = g union tmp.dfg //(dfg union tmp.dfg)
        for (i <- 2 until seq.size) {
          tmp = f(tmp, seq(i))
          g = g union tmp.dfg //(dfg union tmp.dfg)
        }
      } /*else {
        g = dfg
      }*/
      new FoldR(g)
    }
    def apply(seq: Component*)(f: (Component, Component) => Component):
    FoldR = buildFoldR(seq,f)
    def apply(seq: List[Component])(f: (Component, Component) => Component):
    FoldR = buildFoldR(seq,f)
  }

  case class ZipWith(dfg: Graph[DfgNode, DfgEdge], comps : List[Component]) extends DataPattern

  object ZipWith {
    def buildZipWidth(seq1: Seq[Component], seq2: Seq[Component], f: (Component, Component) => Component): ZipWith = {
      var outs = List[Component]()
      var g = Graph.empty[DfgNode, DfgEdge]
      for (i <- 0 until seq1.size) {
        val tmp = f(seq1(i), seq2(i))
        outs = outs ::: List(tmp)
        val leafNode = tmp.dfg.getLeaves.head
        val leaf = leafNode match {
          case arithNode: ArithNode => Some(arithNode)
          case _ => None
        }
        val arithNode = ArithNode(leaf.get.name, leaf.get.lhs, leaf.get.rhs, getArith2ArgNodeWidth(leaf.get.operator, leaf.get.lhs.width, leaf.get.rhs.width), leaf.get.operator, 1)
        val diPredNodes = tmp.dfg.getDiPredNodes(leafNode)
        val diPredLhs = diPredNodes.head
        val diPredRhs = diPredNodes.last

        // determine the source ports
        val diPredLhsSrcPort = diPredLhs match {
          case cmpNode: CompNode => {
            if (!cmpNode.comp.isInstanceOf[ConstVal]) cmpNode.comp._outports.find(_.typ == PortTypeEnum.DOUT)
            else {
              val constVal = cmpNode.comp.asInstanceOf[ConstVal]
              Some(Streamer(constVal.name, constVal.level, constVal.width, Some(constVal.vl)))
            }
          }
          case tmplNode: TmplNode => {
            if (!tmplNode.comp.isInstanceOf[ConstVal]) tmplNode.comp._outports.find(_.typ == PortTypeEnum.DOUT)
            else {
              val constVal = tmplNode.comp.asInstanceOf[ConstVal]
              Some(Streamer(constVal.name, constVal.level, constVal.width, Some(constVal.vl)))
            }
          }
          case _ => None
        }

        val diPredRhsSrcPort = diPredRhs match {
          case cmpNode: CompNode => {
            if (!cmpNode.comp.isInstanceOf[ConstVal]) cmpNode.comp._outports.find(_.typ == PortTypeEnum.DOUT)
            else {
              val constVal = cmpNode.comp.asInstanceOf[ConstVal]
              Some(Streamer(constVal.name, constVal.level, constVal.width, Some(constVal.vl)))
            }
          }
          case tmplNode: TmplNode => {
            if (!tmplNode.comp.isInstanceOf[ConstVal]) tmplNode.comp._outports.find(_.typ == PortTypeEnum.DOUT)
            else {
              val constVal = tmplNode.comp.asInstanceOf[ConstVal]
              Some(Streamer(constVal.name, constVal.level, constVal.width, Some(constVal.vl)))
            }
          }
          case _ => None
        }

        val lhsEdge = DfgEdge[DfgNode](diPredLhs, arithNode, diPredLhs.inst + "_" + arithNode.name, diPredLhsSrcPort.getOrElse(null), null)
        val rhsEdge = DfgEdge[DfgNode](diPredRhs, arithNode, diPredRhs.inst + "_" + arithNode.name, diPredRhsSrcPort.getOrElse(null), null)

        // val tmp1 = dfg union Graph[DfgNode, DfgEdge](lhsEdge, rhsEdge)
        g = g union Graph[DfgNode, DfgEdge](lhsEdge, rhsEdge)
      }

      new ZipWith(g, outs)
    }

   // def apply(seq1: Component*)(seq2: Component*)(f: (Component, Component) => Component):
   // ZipWith = buildZipWidth(seq1, seq2, f)

   // def apply(seq1: Seq[Component])(seq2: Component*)(f: (Component, Component) => Component):
   // ZipWith = buildZipWidth(seq1, seq2, f)

    def apply(seq1: Seq[Component])(consts: Constants)(f: (Component, Component) => Component):
    ZipWith = buildZipWidth(seq1, consts.comps, f)
  }
}
