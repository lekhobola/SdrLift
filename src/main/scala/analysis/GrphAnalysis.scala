package sdrlift.analysis


import exp.CompExp.{Comp, Component, Delay}
import exp.NodeExp
import exp.NodeExp.Streamer
import scalax.collection.Graph
import scalax.collection.GraphTraversal._
import scalax.collection.edge.Implicits._
import sdrlift.graph.Dfg._
import sdrlift.graph.NodeFactory._
import sdrlift.graph.{DfgEdge, DfgNode}
import NodeExp._

object GrphAnalysis {

  case class ModuleProps(et: Int, pr: Int, cr: Int, pp: List[(Int, String)], cp: List[(Int, String)], vldNode: DfgNode)

  def removeExtraEdges(g: Graph[DfgNode, DfgEdge], inst: String) = { //g: Graph[DfgNode, DfgEdge]
    var grph = g
    // find redundant directed predecessor edges for a leaf (CombNode or SeqNode)
    val leaf = grph.getLeaves.head
    if (leaf.isInstanceOf[DirNode]) {
      val diPredEdges = (grph get leaf).incoming
      diPredEdges.foreach { e =>
        if (e.source.diSuccessors.size > 1) {
          grph = grph - e
        }
      }
    }
    // draw compiler DFG
    //grph.draw(cmp.inst)
    grph
  }

  def removeDiNode(g: Graph[DfgNode, DfgEdge], inst: String) = { //g: Graph[DfgNode, DfgEdge]
    var grph = g
    val leaf = grph.getLeaves.head

    // draw the the schematic
    if (leaf.isInstanceOf[DirNode]) {
      val diPredEdges = (grph get leaf).incoming
      grph = grph -- diPredEdges
      grph = grph - leaf
    }
    // grph.schematic(inst)
    // println(grph.order)
    // draw IR DFG
    // grph.drawIR(inst)
    grph
  }

  def getCompProps(cmp: Component) = {
    def getProps(cmp: Component, lastNode: DfgNode = null): (Int,DfgNode) = {
      val opt1_grph = removeExtraEdges(cmp.dfg, "analys1")
      val opt2_grph = removeDiNode(opt1_grph, "analys2")

      val rootOpt = if(cmp.iopaths.isEmpty) Some(opt2_grph.getRoots.head) else opt2_grph.getRoots.find(_.inst.equals(cmp.iopaths.head._1.getLastNodeId))
      val leafOpt = if(cmp.iopaths.isEmpty) Some(opt2_grph.getLeaves.head) else opt2_grph.getLeaves.find(_.inst.equals(cmp.iopaths.head._2.getLastNodeId))
      val dfg = cmp.dfg

      val sp = (dfg get rootOpt.get) shortestPathTo (dfg get leafOpt.get)
      var grph = Graph.empty[DfgNode, DfgEdge]
      grph = if (sp != None) {
        if (sp.get.nodes.size == 1) grph + sp.get.nodes.head else grph ++ sp.get.edges
      } else grph
      val nodes = grph.getOrderedNodes
      val list: List[(Int,DfgNode)] =
        nodes.map { n =>
          n match {
            case tmplNode: TmplNode => {
              tmplNode.comp match {
                case del: Delay => {
                  (del.depth, tmplNode)
                }
                case _ => (0, lastNode)
              }
            }
            case compNode: CompNode => {
              val cycle = opt2_grph.findCycleContaining(opt2_grph get compNode)
              val cycleEls = if (cycle != None) cycle.get.edges else List.empty
              if (!cycleEls.isEmpty) {
                var cycle_grph = Graph.empty[DfgNode, DfgEdge]
                cycle_grph = cycle_grph ++ cycleEls
                cycle_grph = cycle_grph - compNode
                val cycle_root = cycle_grph.getRoots.head
                val cycle_leaf = cycle_grph.getLeaves.head
                case class TmpCmp(inst: String) extends Component {
                  override val iopaths = List()
                  override val dfg = cycle_grph
                  override val name = "dummy"
                }
                val tmpCmp = TmpCmp("dummyInst")
                getProps(tmpCmp, compNode)
              }else {
                getProps(compNode.comp, compNode)
              }
            }
            case _ => (0, lastNode)
          }
        }
      val filtList = list.filter(_._2 != null)
      if (!filtList.isEmpty) (filtList.map(_._1).reduceLeft(_ + _), filtList.last._2) else (0, null)
    }

   // grph.draw("path")
    val props = getProps(cmp)
    val d : Int = props._1
    val et = (d * 2) + 1
    val pr = d + 1
    val cr = pr
    val pp = List((pr, "1"), (d, "0"))
    val cp = List((d, "0"), (cr, "1"))
    ModuleProps(et, pr, cr, pp, cp, props._2)
  }
}
