package sdrlift.codegen.vhdl.template

import sdrlift.codegen.vhdl.VhdlCodeGen
import de.upb.hni.vmagic.VhdlFile
import de.upb.hni.vmagic.builtin.StdLogic1164
import de.upb.hni.vmagic.concurrent.ConditionalSignalAssignment
import de.upb.hni.vmagic.concurrent.ProcessStatement
import de.upb.hni.vmagic.declaration.{Attribute, SignalDeclaration, VariableDeclaration}
import de.upb.hni.vmagic.expression._
import de.upb.hni.vmagic.highlevel.Register
import de.upb.hni.vmagic.libraryunit.Architecture
import de.upb.hni.vmagic.libraryunit.Entity
import de.upb.hni.vmagic.libraryunit.LibraryClause
import de.upb.hni.vmagic.libraryunit.UseClause
import de.upb.hni.vmagic.`object`.{AttributeExpression, Constant, Signal, Variable}
import de.upb.hni.vmagic.statement.{ForStatement, IfStatement, SignalAssignment, VariableAssignment}
import de.upb.hni.vmagic.`type`.{ConstrainedArray, RangeSubtypeIndication, SubtypeIndication, UnconstrainedArray}
import de.upb.hni.vmagic._
import de.upb.hni.vmagic.Range
import de.upb.hni.vmagic.`object`.VhdlObject.Mode
import de.upb.hni.vmagic.builtin.StdLogicSigned
import de.upb.hni.vmagic.builtin.StdLogicUnsigned
import de.upb.hni.vmagic.builtin.Standard
import de.upb.hni.vmagic.literal.{DecimalLiteral, StringLiteral}

case class FifoTp(params : Map[String,Any]) extends VhdlCodeGen {
  val width: Int = params.get("width").get.asInstanceOf[Int]
  val depth: Int = params.get("depth").get.asInstanceOf[Int]
  val inst = params.get("inst").get

  private val file: VhdlFile = new VhdlFile
  private val entity: Entity = new Entity("fifo")
  private val architecture: Architecture = new Architecture("rtl", entity)

  private val hdlWidthGeneric: Constant = new Constant("WIDTH", Standard.POSITIVE, new DecimalLiteral(width))
  private val hdlDepthGeneric: Constant = new Constant("DEPTH", Standard.POSITIVE, new DecimalLiteral(depth))
  private val hdlClkSgnl: Signal = new Signal("clk", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlRstSgnl: Signal = new Signal("rst", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlWeSgnl: Signal = new Signal("we", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlDinSgnl: Signal = new Signal("din", Mode.IN, StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric));
  private val hdlReSgnl: Signal = new Signal("re", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlDoutSgnl: Signal = new Signal("dout", Mode.OUT, StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric));
  private val hdlEmSgnl: Signal = new Signal("em", Mode.OUT, StdLogic1164.STD_LOGIC)
  private val hdlFlSgnl: Signal = new Signal("fl", Mode.OUT, StdLogic1164.STD_LOGIC)
  private val hdlVldSgnl: Signal = new Signal("vld", Mode.OUT, StdLogic1164.STD_LOGIC)

  /**
    * creates the VHDL file
    *
    * @return A VhdlFile containing the implementation
    */
  override def getHdlFile(libClauses: List[String], useClauses: List[String]): VhdlFile = {
    //file = new VhdlFile();
    // Add Elements
    file.getElements.add(new LibraryClause("IEEE"))
    file.getElements.add(StdLogic1164.USE_CLAUSE)
    file.getElements.add(StdLogicUnsigned.USE_CLAUSE)

    file.getElements.add(getHdlEntity)
    file.getElements.add(getHdlArchitecture)
    file
  }

  /**
    * Create the entity of the template
    *
    * @return the entity
    */
  override def getHdlEntity: Entity = {
    entity.getGeneric.add(hdlWidthGeneric)
    entity.getGeneric.add(hdlDepthGeneric)
    entity.getPort.add(hdlClkSgnl)
    entity.getPort.add(hdlRstSgnl)


    entity.getPort.add(hdlWeSgnl)
    entity.getPort.add(hdlDinSgnl)
    entity.getPort.add(hdlReSgnl)
    entity.getPort.add(hdlDoutSgnl)
    entity.getPort.add(hdlEmSgnl)
    entity.getPort.add(hdlFlSgnl)
    entity.getPort.add(hdlVldSgnl)

    entity
  }

  /**
    * Implement the counter, declare internal signals etc.
    *
    * @return the architecture containing the template implementation
    */
  override def getHdlArchitecture: Architecture = {
    // the architecture requires the associated entity
    //architecture =

    // a process to determine the next register contents
    val hdlProc: ProcessStatement = new ProcessStatement("fifo_proc")

    // generation of internal register signals
    val hdlMemType = new ConstrainedArray("mem_type", StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric),
      new Range(new DecimalLiteral(0), Range.Direction.TO,
        new Subtract(hdlDepthGeneric, new DecimalLiteral(1))));
    hdlProc.getDeclarations.add(hdlMemType)
    val hdlMemVrbl = new Variable("memory", hdlMemType,
      Aggregate.OTHERS(Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
    hdlProc.getDeclarations.add(new VariableDeclaration(hdlMemVrbl))

    val hdlDepthRangeProv : RangeProvider[Range] = (new Range(new DecimalLiteral(0), Range.Direction.TO,
      new Subtract(hdlDepthGeneric, new DecimalLiteral(1)))).asInstanceOf[RangeProvider[Range] ]
    val hdlEndsType = new RangeSubtypeIndication(Standard.NATURAL, hdlDepthRangeProv)
    val hdlHeadVrbl = new Variable("head", hdlEndsType)
    hdlProc.getDeclarations.add(new VariableDeclaration(hdlHeadVrbl))

    val hdlTailVrbl = new Variable("tail", hdlEndsType)
    hdlProc.getDeclarations.add(new VariableDeclaration(hdlTailVrbl))

    val hdlLoopedVrbl = new Variable("looped", Standard.BOOLEAN)
    hdlProc.getDeclarations.add(new VariableDeclaration(hdlLoopedVrbl))

    val hdlEmptyVrbl = new Variable("empty", StdLogic1164.STD_LOGIC,StdLogic1164.STD_LOGIC_0)
    hdlProc.getDeclarations.add(new VariableDeclaration(hdlEmptyVrbl))

    // the process contains an if block
    val hdlProcIf: IfStatement = new IfStatement(new Equals(hdlRstSgnl, StdLogic1164.STD_LOGIC_1))
    hdlProcIf.getStatements.add(new VariableAssignment(hdlHeadVrbl, new DecimalLiteral(0)))
    hdlProcIf.getStatements.add(new VariableAssignment(hdlTailVrbl, new DecimalLiteral(0)))
    hdlProcIf.getStatements.add(new VariableAssignment(hdlLoopedVrbl, Standard.BOOLEAN_FALSE))
    hdlProcIf.getStatements.add(new SignalAssignment(hdlFlSgnl, StdLogic1164.STD_LOGIC_0))
    hdlProcIf.getStatements.add(new VariableAssignment(hdlEmptyVrbl, StdLogic1164.STD_LOGIC_1))

    hdlProcIf.createElsifPart(new And(new AttributeExpression[Signal](hdlClkSgnl,
      new Attribute("EVENT", null)), new Equals(hdlClkSgnl, StdLogic1164.STD_LOGIC_1)))
    hdlProcIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_0))
    hdlProcIf.getElsifParts.get(0).getStatements.
      add(new SignalAssignment(hdlDoutSgnl,  Aggregate.OTHERS(Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0))))

    val hdlIf: IfStatement = new IfStatement(
      new And(new And(new And(new And(new Equals(hdlReSgnl, StdLogic1164.STD_LOGIC_1),
        new Equals(hdlWeSgnl, StdLogic1164.STD_LOGIC_1)), new Equals(hdlEmptyVrbl, StdLogic1164.STD_LOGIC_1)),
        new Equals(hdlHeadVrbl, hdlTailVrbl)), new Equals(hdlLoopedVrbl, Standard.BOOLEAN_FALSE)))
    hdlIf.getStatements.add(new SignalAssignment(hdlDoutSgnl, hdlDinSgnl))
    hdlIf.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_1))

    // if statmeente - 1
    val hdlIf1: IfStatement = new IfStatement(new Equals(hdlReSgnl, StdLogic1164.STD_LOGIC_1))

    val hdlIf1If: IfStatement = new IfStatement(new Or(new Equals(hdlLoopedVrbl, Standard.BOOLEAN_FALSE), new NotEquals(hdlHeadVrbl, hdlTailVrbl)) )
    hdlIf1If.getStatements.add(new SignalAssignment(hdlDoutSgnl, hdlMemVrbl.getArrayElement(hdlTailVrbl)))
    hdlIf1If.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_1))

    val hdlIf1IfIf: IfStatement = new IfStatement(new Equals(hdlTailVrbl, new Subtract(hdlDepthGeneric, new DecimalLiteral(1))))
    hdlIf1IfIf.getStatements.add(new VariableAssignment(hdlTailVrbl, new DecimalLiteral(0)))
    hdlIf1IfIf.getStatements.add(new VariableAssignment(hdlLoopedVrbl, Standard.BOOLEAN_FALSE))
    hdlIf1IfIf.getElseStatements.add(new VariableAssignment(hdlTailVrbl, new Add(hdlTailVrbl, new DecimalLiteral(1))))

    hdlIf1If.getStatements.add(hdlIf1IfIf)

    hdlIf1.getStatements.add(hdlIf1If)
    hdlIf.getElseStatements.add(hdlIf1)

    // if statmeente - 2
    val hdlIf2: IfStatement = new IfStatement(new Equals(hdlWeSgnl, StdLogic1164.STD_LOGIC_1))

    val hdlIf2If: IfStatement = new IfStatement(new Or(new Equals(hdlLoopedVrbl, Standard.BOOLEAN_FALSE), new NotEquals(hdlHeadVrbl, hdlTailVrbl)) )
    hdlIf2If.getStatements.add(new VariableAssignment(hdlMemVrbl.getArrayElement(hdlHeadVrbl), hdlDinSgnl))

    val hdlIf2IfIf: IfStatement = new IfStatement(new Equals(hdlHeadVrbl, new Subtract(hdlDepthGeneric, new DecimalLiteral(1))))
    hdlIf2IfIf.getStatements.add(new VariableAssignment(hdlHeadVrbl, new DecimalLiteral(0)))
    hdlIf2IfIf.getStatements.add(new VariableAssignment(hdlLoopedVrbl, Standard.BOOLEAN_TRUE))
    hdlIf2IfIf.getElseStatements.add(new VariableAssignment(hdlHeadVrbl, new Add(hdlHeadVrbl, new DecimalLiteral(1))))

    hdlIf2If.getStatements.add(hdlIf2IfIf)

    hdlIf2.getStatements.add(hdlIf2If)
    hdlIf.getElseStatements.add(hdlIf2)

    // if statmeente - 3
    val hdlIf3: IfStatement = new IfStatement(new Equals(hdlHeadVrbl, hdlTailVrbl))

    val hdlIf3If: IfStatement = new IfStatement(hdlLoopedVrbl)
    hdlIf3If.getStatements.add(new SignalAssignment(hdlFlSgnl, StdLogic1164.STD_LOGIC_1))
    hdlIf3If.getElseStatements.add(new VariableAssignment(hdlEmptyVrbl, StdLogic1164.STD_LOGIC_1))

    hdlIf3.getElseStatements.add(new VariableAssignment(hdlEmptyVrbl, StdLogic1164.STD_LOGIC_0))
    hdlIf3.getElseStatements.add(new SignalAssignment(hdlFlSgnl, StdLogic1164.STD_LOGIC_0))
    hdlIf3.getStatements.add(hdlIf3If)
    hdlIf.getElseStatements.add(hdlIf3)

   /* if (Head = Tail) then
        if Looped then
          fl <= '1';
        else
          empty := '1';
        end if;
      else
        empty := '0';
        fl <= '0';
      end if; */

    hdlProcIf.getElsifParts.get(0).getStatements.add(hdlIf)
    hdlProcIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(hdlEmSgnl, hdlEmptyVrbl))
    hdlProc.getStatements.add(hdlProcIf)

    //  architecture.getStatements.add(new ConditionalSignalAssignment(hdlDoutSgnl,
    //    hdlMemVrbl.getArrayElement(new Subtract(hdlDepthGeneric, new DecimalLiteral(1)))))

    // modify the sensitivity list
    hdlProc.getSensitivityList.add(hdlClkSgnl)
    hdlProc.getSensitivityList.add(hdlRstSgnl)

    // add the process to the architecture
    architecture.getStatements.add(hdlProc)

    // forward the register output to the entity output
    // architecture.getStatements.add(new ConditionalSignalAssignment(output, count))

    architecture
  }
}
