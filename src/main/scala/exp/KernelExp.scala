package exp

import exp.CompExp.Component
import exp.NodeExp.Streamer
import exp.PatternsExp.{Chain, FoldR, ZipWith}
import scalax.collection.Graph
import sdrlift.graph.{Dfg, DfgEdge, DfgNode, NodeFactory}
import sdrlift.graph.NodeFactory.{DirNode, Node, PortTypeEnum}

import scala.collection.Seq
import scalax.collection.Graph
import scalax.collection.edge.Implicits._
import sdrlift.graph.Dfg._

object KernelExp {

  abstract class Kernel extends Component {
    val name: String
    val inst: String

    override def model(elems: Seq[Any]): Graph[DfgNode, DfgEdge] = {
      var g = Graph.empty[DfgNode, DfgEdge]
      val elemList = elems.toList.distinct

      elemList.foreach { s =>
        (s) match {
          case (chn: Chain[Component]) => g = g ++ chn.dfg
          case (rdc: FoldR) => g = g ++ rdc.dfg
          case (zw: ZipWith) => g = g ++ zw.dfg
          case _ => g = g ++ super.model(Seq(s))
        }
      }

      if (g.nodes.toList.find(_.isInstanceOf[DirNode]) == None) {
        val cmbNode = DirNode("cmb_" + NodeFactory.randomStr(6), "Combinational")
        val leaves = g.getLeaves
        var list: List[DfgEdge[DfgNode]] = List()
        leaves.foreach { el =>
          list = list ::: List[DfgEdge[DfgNode]](DfgEdge[DfgNode](el, cmbNode,
            el.inst + "_" + cmbNode.name, null, null))
        }
        g = g ++ list
      }
      //g.draw(inst)
      g
    }
  }

  abstract class Module extends Kernel

  abstract class Macro extends Kernel {
    def addParam(name: String, vl: Any): Unit = {
      _params += Tuple2(name, vl)
    }

    def addInPort(name: String, typ: PortTypeEnum.PortTypeEnum, ap: List[(Int, String)] = null, width: Int = 0): Unit = {
      _inports = _inports ::: List(Streamer(name, 0, width, null, ap, typ, -1, null))
    }

    def addOutPort(name: String, typ: PortTypeEnum.PortTypeEnum, ap: List[(Int, String)] = null): Unit = {
      _outports == _outports ::: List(Streamer(name, 0, width, null, ap, typ, -1, null))
    }
  }

  case class KernelPort(val rate: Int, val ap: List[(Int, String)], val labels : java.util.HashMap[String,String]) {
    override def toString = "KernelPort(Rate: " + rate + ", accessPattern" + ap + ", labels" + labels + ")"
  }
}
