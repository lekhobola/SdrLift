package sdrlift.analysis


import exp.CompExp.{Comp, Component, Delay, DownSampler}
import exp.NodeExp
import NodeExp.{Streamer, _}
import scalax.collection.Graph
import scalax.collection.GraphTraversal._
import scalax.collection.edge.Implicits._
import sdrlift.graph.Dfg._
import sdrlift.graph.NodeFactory._
import sdrlift.graph.{DfgEdge, DfgNode}
import exp.KernelExp.Module
import sdrlift.analysis.GrphAnalysis.ModelProps

object GrphAnalysis {

  case class ModelProps(et: Int, cr: Int, pr: Int, cp: List[(Int, String)], pp: List[(Int, String)])

  def removeExtraEdges(g: Graph[DfgNode, DfgEdge], inst: String) = {
    // g: Graph[DfgNode, DfgEdge]
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

  /*def getCompProps(comp: Component) = {

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
                case ds: DownSampler => {
                  (ds.rateChange, tmplNode)
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
    println(comp)
    val props = getProps(comp)
    val d : Int = props._1
    val cr = comp.asInstanceOf[Module].input_length
    val pr = comp.asInstanceOf[Module].output_length
    val cp = List((cr, "1"), (d, "0"))
    val pp = List((d, "0"), (cr, "1"))
    val et = cr + d
    ModelProps(et, cr, pr, cp, pp, props._2)
  } */

  def getModelProps(comp: Component, iopath: (Streamer, Streamer)): ModelProps = {

    def getProps(cmp: Component, iopath: (Streamer, Streamer), props: ModelProps): ModelProps = {
      val opt1_grph = removeExtraEdges(cmp.dfg, "analys1")
      val opt2_grph = removeDiNode(opt1_grph, "analys2")

      val rootOpt = if (cmp.iopaths.isEmpty) Some(opt2_grph.getRoots.head) else opt2_grph.getRoots.find(_.inst.equalsIgnoreCase(cmp.iopaths.head._1.getLastNodeId))
      val leafOpt = if (cmp.iopaths.isEmpty) Some(opt2_grph.getLeaves.head) else opt2_grph.getLeaves.find(_.inst.equalsIgnoreCase(cmp.iopaths.head._2.getLastNodeId))
      val dfg = cmp.dfg

      val sp = if(rootOpt != None && leafOpt != None) (dfg get rootOpt.get) shortestPathTo (dfg get leafOpt.get) else None
      var grph = Graph.empty[DfgNode, DfgEdge]
      grph = if (sp != None) {
        if (sp.get.nodes.size == 1) grph + sp.get.nodes.head else grph ++ sp.get.edges
      } else grph
      val nodes = grph.getOrderedNodes
      var propsTmp : ModelProps = props

      nodes.foreach { n =>
        n match {
          case tmplNode: TmplNode => {
            tmplNode.comp match {
              case del: Delay => {
                val d = props.cp.last._1 + del.depth
                val cr = comp.asInstanceOf[Module].input_length
                val pr = comp.asInstanceOf[Module].output_length
                val cp = List((cr, "1"), (d, "0"))
                val pp = List((d, "0"), (pr, "1"))
                val et = cr + d
                propsTmp = ModelProps(et, cr, pr, cp, pp)
              }
              case ds: DownSampler => {

                val cr = ds.rateChange
                val pr = 1
                val cp = List((ds.rateChange, "1"))
                val pp = List((ds.rateChange - 1, "0"), (1, "1"))
                val et = cr
                propsTmp = ModelProps(et, cr, pr, cp, pp)
              }
              case _ => //propsTmp = ModelProps(props.et, props.cr, props.pr, props.cp, props.pp)
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
              propsTmp = getProps(tmpCmp, null,propsTmp)
            } else {
              propsTmp = getProps(compNode.comp, null, propsTmp)
            }
          }
          case _ => //propsTmp = ModelProps(props.et, props.cr, props.pr, props.cp, props.pp)
        }
      }
      propsTmp
    }

    // grph.draw("path")
    //val props = getProps(comp)
    val cr = 1
    val pr = 1
    val cp = List((1, "1"), (0, "0"))
    val pp = List((0, "0"), (1, "1"))
    val et = 1
    getProps(comp, iopath, ModelProps(et, cr, pr, cp, pp))

  }
}
