package sdrlift.codegen.vhdl

import de.upb.hni.vmagic.{AssociationElement, SubtypeDiscreteRange, VhdlFile, expression}
import de.upb.hni.vmagic.`object`.{AttributeExpression, Constant, Signal}
import de.upb.hni.vmagic.builtin.{Standard, StdLogic1164, StdLogicSigned}
import de.upb.hni.vmagic.concurrent.{ComponentInstantiation, ConditionalSignalAssignment, ProcessStatement}
import de.upb.hni.vmagic.declaration.{Attribute, Component, ConstantDeclaration, SignalDeclaration}
import de.upb.hni.vmagic.libraryunit.{Architecture, Entity, LibraryClause, UseClause}
import de.upb.hni.vmagic.statement.{CaseStatement, IfStatement, SignalAssignment}
import de.upb.hni.vmagic.`object`.VhdlObject.Mode
import scalax.collection.GraphPredef._
import scalax.collection.GraphEdge._
import scalax.collection.Graph
import sdrlift.graph._
import Dfg._
import DfgEdge.ImplicitEdge
import de.upb.hni.vmagic.literal.{BasedLiteral, BinaryLiteral, DecimalLiteral}
import NodeFactory._
import sdrlift.analysis.GrphAnalysis
import de.upb.hni.vmagic.expression._
import de.upb.hni.vmagic.output.VhdlOutput
import de.upb.hni.vmagic.util.VhdlCollections
import exp.CompExp.{Combinational, Sequential}
import exp.KernelExp.Module
import scalax.collection.GraphTraversal.{BreadthFirst, DepthFirst, Predecessors}
import sdrlift.analysis.GrphAnalysis._

import scala.collection.immutable.Map
//import de.upb.hni.vmagic.expression.{Add, Subtract, Divide, Or, And, Multiply, LessThan}
import de.upb.hni.vmagic.highlevel.StateMachine

import scala.collection.JavaConversions._

abstract class VhdlNodeCodeGen(comp: exp.CompExp.Component) extends VhdlCodeGen {
  private val outdir = "out/vhdl/"
  private val dfg = removeExtraEdges(comp.dfg, comp.inst)
  private val roots = dfg.getRoots
  private val leaves = dfg.getLeaves
  private val branches = dfg.getBranches
  private val vhdlPrimitives = VhdlPrimitives(dfg)

  private val file: VhdlFile = new VhdlFile
  private val entity: Entity = new Entity(comp.name)
  private val architecture: Architecture = new Architecture("rtl", entity)

  private val topInNodes = roots.map { r =>
    r match {
      case compNode: CompNode => compNode.comp.dfg.getRoots.map { n => (r, n) }
      case tmplNode: TmplNode => {
        if (tmplNode.comp._inports != null) tmplNode.comp._inports.map(n => (r, n)) else List.empty
      }
      case _ => List.empty
    }
  }.flatten.distinct
  //private val topOutNodes = leaves.map { l =>
  private val topOutNodes = dfg.getDiPredNodes(leaves.head).map { l =>
    l match {
      case compNode: CompNode => compNode.comp.dfg.getArithLeafNodes.map { n => (l, n) }
      case tmplNode: TmplNode => tmplNode.comp._outports.map(n => (l, n))
      case arithNode: ArithNode => List((arithNode, arithNode))
      case _ => List.empty
    }
  }.flatten.distinct

  /**
    * creates the VHDL file and adds Designunits to it
    *
    * @param wordWidth the width of the Componet
    * @return A VhdlFile containing the implementation
    */
  override def getHdlFile(libClauses: List[String], useClauses: List[String]): VhdlFile = {
    //file = new VhdlFile();
    // Add Elements
    file.getElements.add(new LibraryClause("IEEE"))
    file.getElements.add(StdLogic1164.USE_CLAUSE)
    file.getElements.add(StdLogicSigned.USE_CLAUSE)

    if (libClauses != null) {
      for (libClause <- libClauses)
        file.getElements.add(new LibraryClause(libClause))
    }

    if (useClauses != null) {
      for (useClause <- useClauses)
        file.getElements.add(new UseClause(useClause))
    }

    file.getElements.add(entity)
    file.getElements.add(getHdlArchitecture)
    file
  }

  /**
    * Create the entity of the Component
    *
    * @return the entity
    */
  override def getHdlEntity: Entity = {

    entity
  }


  /**
    * Implement the Component, declare internal signals etc.
    *
    * @return the architecture containing the Component implementation
    */
  override def getHdlArchitecture: Architecture = {
    // add input data ports
    roots.foreach { n =>
      (n) match {
        //case (stm: StrmNode) => entity.getPort.add(new Signal(stm.name, Mode.IN, if (stm.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(stm.width)))
        case _ => null
      }
    }

    // add output data ports
    // dfg.getArithLeafNodes.foreach { n =>
    dfg.getDiPredNodes(leaves.head).foreach { n =>
      (n) match {
        //case (arith: ArithNode) => entity.getPort.add(new Signal(arith.name, Mode.OUT, if (arith.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(arith.width)))
        case _ =>
      }
    }

    // the architecture requires the associated entity
    //generation of internal constant registers
    roots.foreach { n =>
      (n) match {
        case (cnst: ConstNode) => {
          val c = new Constant(cnst.name, Standard.INTEGER, new DecimalLiteral(cnst.vl))
          //architecture.getDeclarations.add(new ConstantDeclaration(c))
        }
        case _ => null
      }
    }

    // generation of internal register signals
    branches.foreach { n =>
      if (n.isInstanceOf[StrmNode] || n.isInstanceOf[ArithNode]) {
        val s = new Signal(n.name, StdLogic1164.STD_LOGIC_VECTOR(n.width), Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0))
        // architecture.getDeclarations.add(new SignalDeclaration(s))
      }
    }

    // TODO: Add the process if the control nodes are available
    val hdlProc: ProcessStatement = new ProcessStatement(comp.name + "_proc")
    val clk: Signal = new Signal("clk", Mode.IN, StdLogic1164.STD_LOGIC)
    val rst: Signal = new Signal("rst", Mode.IN, StdLogic1164.STD_LOGIC)
    val hdlProcIf: IfStatement = new IfStatement(new Equals(rst, StdLogic1164.STD_LOGIC_1))

    var stateNodes: List[DfgNode] = List()

    /* if (leaves.exists(p => p.isInstanceOf[DirNode])) { // only add rst, clk when code is sequential
      entity.getPort.add(clk)
      entity.getPort.add(rst)
      // add the IfStatement for rising edge of the clock
      hdlProcIf.createElsifPart(
        new And(new AttributeExpression[Signal](clk, new Attribute("EVENT", null)),
          new Equals(clk, StdLogic1164.STD_LOGIC_1)))
    }*/

    // Add the control nodes such as FsmNode, IfNode, etc ...
    leaves.foreach { leafNode =>
      (leafNode) match {
        case dirNode: DirNode => {
          (dirNode.ctrl) match {
            // sequential nodes
            case "Sequential" => {
              val cmbNodes = dfg.getDiPredNodes(dirNode, List("fsm", "sx", "if"))
              cmbNodes.foreach { n =>
                (n) match {
                  case (fsmNode: FsmNode) => {
                    stateNodes = dfg.getPredecessorNodes(fsmNode, List("sx"))
                    val fsmLength = stateNodes.size
                    val fsmLog2 = Math.ceil(Math.log10(fsmLength) / Math.log10(2)).toInt

                    // Signal Declaration
                    val hdlStateSgnl = new Signal(fsmNode.name, StdLogic1164.STD_LOGIC_VECTOR(fsmLog2),
                      new BinaryLiteral((fsmNode.firstState).toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString))
                    architecture.getDeclarations.add(new SignalDeclaration(hdlStateSgnl))

                    // Reset signals in process
                    hdlProcIf.getStatements.add(new SignalAssignment(hdlStateSgnl, Aggregate.OTHERS
                    (StdLogic1164.STD_LOGIC_0)))

                    hdlProcIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(hdlStateSgnl, Aggregate.OTHERS
                    (StdLogic1164.STD_LOGIC_0)))
                    val hdlCst = new CaseStatement(hdlStateSgnl) // FSM

                    for (i <- 0 until fsmLength) {
                      val stateNode = stateNodes.find(_.level == i).get
                      val hdlState = hdlCst.createAlternative(new BinaryLiteral(i.toBinaryString.reverse.padTo(fsmLog2,
                        "0")
                        .reverse.mkString))
                      val predBlocks = dfg.getCodeBlocksFiltNot(stateNode, List("sx", "lgc", "cnd"))
                      // add the state code block
                      vhdlPrimitives.getExpLines(predBlocks).foreach { e =>
                        hdlState.getStatements.add(vhdlPrimitives.getAlSA(e._1, e._2, e._3))
                      }

                      // add the general if-else statements
                      val ifsPredBlocks = predBlocks.filter(e => e.prefix.equalsIgnoreCase("if"))
                      for (ifn <- ifsPredBlocks) {
                        hdlState.getStatements.add(vhdlPrimitives.buildIfStmt(ifn))
                      }

                      // add the next-state if statements
                      val nxtStCondNodeList = dfg.getDiPredNodes(stateNode, List("cnd"))

                      for (nxStCondNode <- nxtStCondNodeList) {
                        val lgcNode = dfg.getDiPredNodes(nxStCondNode, List("lgc")).head
                        val hdlCondOpExp = vhdlPrimitives.buildCondBinHdl(lgcNode)
                        val hdlNextStateIf: IfStatement = new IfStatement(hdlCondOpExp.asInstanceOf[BinaryExpression[_]])
                        hdlState.getStatements.add(hdlNextStateIf)
                        // next state transition
                        val nxtStNode = dfg.getDiPredNodes(nxStCondNode, List("sx")).head
                        val hdlNxtStSA = new SignalAssignment(hdlStateSgnl, new BinaryLiteral((nxtStNode.level)
                          //TODO: stCondNode.nxtState
                          .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString))
                        hdlNextStateIf.getStatements.add(hdlNxtStSA)
                      }
                    }

                    // Others State
                    val stateX = hdlCst.createAlternative(new BasedLiteral("others"))
                    // TODO: reset all FSM registers here ...
                    // stateX.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(0)))
                    stateX.getStatements.add(new SignalAssignment(hdlStateSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))

                    hdlProcIf.getElsifParts.get(0).getStatements().add(hdlCst)
                  }
                  case (ifNode: IfNode) => {
                    hdlProcIf.getElsifParts.get(0).getStatements.add(vhdlPrimitives.buildIfStmt(ifNode))
                  }
                  case _ => println("Unsynthesizable Sequential Node: " + n + "\n")
                }
              }

              hdlProc.getStatements.add(hdlProcIf)
              architecture.getStatements.add(hdlProc)
            }
            // combinatorial nodes
            case "Combinational" => {
              vhdlPrimitives.combHdlGen(architecture, entity, dfg)

              // add the vld output sgnal
              val props = getCompProps(comp)
              if (props.vldNode != null) {
                val hdlVldSrcSgnl = new Signal(props.vldNode.inst + "_vld", StdLogic1164.STD_LOGIC)
                val hdlVldPortSgnl = new Signal("vld", Mode.OUT, StdLogic1164.STD_LOGIC)
                if(comp.isInstanceOf[Module]) {
                  entity.getPort.add(hdlVldPortSgnl)
                  createPPHdl(hdlVldSrcSgnl, props)
                }else{
                  entity.getPort.add(hdlVldPortSgnl)
                  architecture.getStatements.add(new ConditionalSignalAssignment(hdlVldPortSgnl, hdlVldSrcSgnl))
                }
              }else{
                val hdlVldPortSgnl = new Signal("vld", Mode.OUT, StdLogic1164.STD_LOGIC)
                entity.getPort.add(hdlVldPortSgnl)
                architecture.getStatements.add(new ConditionalSignalAssignment(hdlVldPortSgnl, StdLogic1164.STD_LOGIC_1))
              }
            }
          }
        }
        case _ => println("Unsynthesizable Combinational Node: " + leafNode + "\n")
      }
    }

    //remove all declared signals which are ports or duplicate signals
    val hdlEntityPortSgnls = entity.getPort.toList.distinct.map(_.getVhdlObjects.get(0))
    val hdlEntityPortsSgnlsIds = hdlEntityPortSgnls.map(_.getIdentifier)
    val hdlDeclSgnls = VhdlCollections.getAll(architecture.getDeclarations, classOf[SignalDeclaration])
    var duplicateSgnlIds = List[String]()
    hdlDeclSgnls.foreach { dcl =>
      // println(dcl.getObjects.head.getIdentifier)
      val dclId = dcl.getObjects.head.getIdentifier
      if(hdlEntityPortsSgnlsIds.contains(dclId) || duplicateSgnlIds.count(_.equals(dclId)) > 0)
        architecture.getDeclarations.remove(dcl)

      duplicateSgnlIds = duplicateSgnlIds ::: List(dclId)
    }

    architecture
  }

  def createPPHdl(hdlLastVldSgnl : Signal, props: ModuleProps)={
    // signals
    val hdlClkSgnl: Signal = new Signal("clk", StdLogic1164.STD_LOGIC)
    val hdlRstSgnl: Signal = new Signal("rst", StdLogic1164.STD_LOGIC)
    val hdlEnSgnl: Signal = new Signal("en", StdLogic1164.STD_LOGIC)
    val hdlVldSgnl: Signal = new Signal("vld", StdLogic1164.STD_LOGIC)
    val hdlStateSgnl = new Signal("pp_state", StdLogic1164.STD_LOGIC_VECTOR(2), Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0))
    val hdlCntSgnl: Signal = new Signal("pp_cnt", Standard.INTEGER, new DecimalLiteral(0))
    architecture.getDeclarations.add(new SignalDeclaration(hdlStateSgnl))
    architecture.getDeclarations.add(new SignalDeclaration(hdlCntSgnl))
    architecture.getDeclarations.add(new SignalDeclaration(hdlVldSgnl))

    /** declare a process */
    val hdlProc: ProcessStatement = new ProcessStatement("pp_proc")

    // add a process reset IfStatement
    val hdlProcIf: IfStatement = new IfStatement(new Equals(hdlRstSgnl, StdLogic1164.STD_LOGIC_1))
    // reset process registers inside the reset IfStatement
    hdlProcIf.getStatements.add(new SignalAssignment(hdlStateSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
    hdlProcIf.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_0))
    hdlProcIf.getStatements.add(new SignalAssignment(hdlCntSgnl, new DecimalLiteral(0)))

    // add the IfStatement for rising edge of the clock
    hdlProcIf.createElsifPart(
      new And(new AttributeExpression[Signal](hdlClkSgnl, new Attribute("EVENT", null)),
        new Equals(hdlClkSgnl, StdLogic1164.STD_LOGIC_1)))
    hdlProcIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(hdlStateSgnl, new BinaryLiteral("00")))
    hdlProcIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_0))

    // State

    val hdlFsm = new CaseStatement(hdlStateSgnl)
    val fsms = for (i <- 0 until 3)
      yield hdlFsm.createAlternative(new BinaryLiteral(i.toBinaryString.reverse.padTo(2, "0").reverse.mkString))

    val hdlEnIf: IfStatement = new IfStatement(new Equals(hdlEnSgnl, StdLogic1164.STD_LOGIC_1))
    var stateCount = 0
    for (hdlState <- fsms) {
      if (stateCount == 0) { // First State
        hdlState.getStatements.add(new SignalAssignment(hdlStateSgnl, new BinaryLiteral("00")))
        hdlState.getStatements.add(new SignalAssignment(hdlCntSgnl, new DecimalLiteral(0)))
        hdlState.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_0))
        val hdlState0If: IfStatement = new IfStatement(new Equals(hdlLastVldSgnl, StdLogic1164.STD_LOGIC_1))
        hdlState0If.getStatements.add(new SignalAssignment(hdlStateSgnl, new BinaryLiteral("01")))
        hdlState0If.getStatements.add(new SignalAssignment(hdlCntSgnl, new Add(hdlCntSgnl, new DecimalLiteral(1))))
        hdlState.getStatements.add(hdlState0If)
      } else if (stateCount == 1) {
        hdlState.getStatements.add(new SignalAssignment(hdlStateSgnl, new BinaryLiteral("01")))
        hdlState.getStatements.add(new SignalAssignment(hdlCntSgnl, new Add(hdlCntSgnl, new DecimalLiteral(1))))
        hdlState.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_0))
        val hdlState1If: IfStatement = new IfStatement(new Equals(hdlCntSgnl, new DecimalLiteral(props.cr - 3)))
        hdlState1If.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_1))
        hdlState1If.getStatements.add(new SignalAssignment(hdlStateSgnl, new BinaryLiteral("10")))
        hdlState1If.getStatements.add(new SignalAssignment(hdlCntSgnl, new DecimalLiteral(0)))
        hdlState.getStatements.add(hdlState1If)
      } else if (stateCount == 2) {
        hdlState.getStatements.add(new SignalAssignment(hdlStateSgnl, new BinaryLiteral("10")))
        hdlState.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_1))
        val hdlState2If: IfStatement = new IfStatement(new Equals(hdlLastVldSgnl, StdLogic1164.STD_LOGIC_0))
        hdlState2If.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_0))
        hdlState2If.getStatements.add(new SignalAssignment(hdlStateSgnl, new BinaryLiteral("00")))
        hdlState.getStatements.add(hdlState2If)
      }
      stateCount = stateCount + 1
    }

    val stateX = hdlFsm.createAlternative(new BasedLiteral("others"))
    stateX.getStatements.add(new SignalAssignment(hdlStateSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
    stateX.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_0))
    stateX.getStatements.add(new SignalAssignment(hdlCntSgnl, new DecimalLiteral(0)))


    hdlEnIf.getStatements.add(hdlFsm)
    hdlProcIf.getElsifParts.get(0).getStatements().add(hdlEnIf);

    hdlProc.getStatements.add(hdlProcIf)
    hdlProc.getSensitivityList.add(hdlClkSgnl)
    hdlProc.getSensitivityList.add(hdlRstSgnl)

    architecture.getStatements.add(hdlProc)
  }
}
