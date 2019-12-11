package sdrlift.codegen.vhdl.template

import sdrlift.codegen.vhdl.VhdlCodeGen
import de.upb.hni.vmagic.VhdlFile
import de.upb.hni.vmagic.builtin.StdLogic1164
import de.upb.hni.vmagic.concurrent.ConditionalSignalAssignment
import de.upb.hni.vmagic.concurrent.ProcessStatement
import de.upb.hni.vmagic.declaration.Attribute
import de.upb.hni.vmagic.declaration.SignalDeclaration
import de.upb.hni.vmagic.expression._
import de.upb.hni.vmagic.highlevel.Register
import de.upb.hni.vmagic.libraryunit.Architecture
import de.upb.hni.vmagic.libraryunit.Entity
import de.upb.hni.vmagic.libraryunit.LibraryClause
import de.upb.hni.vmagic.libraryunit.UseClause
import de.upb.hni.vmagic.`object`.AttributeExpression
import de.upb.hni.vmagic.`object`.Signal
import de.upb.hni.vmagic.`object`.Constant
import de.upb.hni.vmagic.statement.{CaseStatement, ForStatement, IfStatement, SignalAssignment}
import de.upb.hni.vmagic.`type`.ConstrainedArray
import de.upb.hni.vmagic.`type`.SubtypeIndication
import de.upb.hni.vmagic.`type`.UnconstrainedArray
import de.upb.hni.vmagic._
import de.upb.hni.vmagic.Range
import de.upb.hni.vmagic.`object`.VhdlObject.Mode
import de.upb.hni.vmagic.builtin.StdLogicSigned
import de.upb.hni.vmagic.builtin.StdLogicUnsigned
import de.upb.hni.vmagic.builtin.Standard
import de.upb.hni.vmagic.literal.{BasedLiteral, BinaryLiteral, DecimalLiteral, StringLiteral}

case class ZeropadTp(params : Map[String,Any]) extends VhdlCodeGen{
  val width: Int = params.get("width").get.asInstanceOf[Int]
  val sampleLength: Int = params.get("sample_length").get.asInstanceOf[Int]
  val padLength: Int = params.get("pad_length").get.asInstanceOf[Int]

  private val file: VhdlFile = new VhdlFile
  private val entity: Entity = new Entity("zero_pad")
  private val architecture: Architecture = new Architecture("rtl", entity)

  private val hdlWidthGeneric: Constant = new Constant("WIDTH", Standard.POSITIVE, new DecimalLiteral(width))
  private val hdlSampleLengthGeneric: Constant = new Constant("SAMPLE_LENGTH", Standard.POSITIVE, new DecimalLiteral(sampleLength))
  private val hdlPadLengthGeneric: Constant = new Constant("PAD_LENGTH", Standard.POSITIVE, new DecimalLiteral(padLength))
  private val hdlClkSgnl: Signal = new Signal("clk", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlRstSgnl: Signal = new Signal("rst", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlEnSgnl: Signal = new Signal("en", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlIinSgnl: Signal = new Signal("iin", Mode.IN, StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric));
  private val hdlQinSgnl: Signal = new Signal("qin", Mode.IN, StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric));
  private val hdlVldSgnl: Signal = new Signal("vld", Mode.OUT, StdLogic1164.STD_LOGIC)
  private val hdlIoutSgnl: Signal = new Signal("iout", Mode.OUT, StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric));
  private val hdlQoutSgnl: Signal = new Signal("qout", Mode.OUT, StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric));

  /**
    * creates the VHDL file
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
    entity.getGeneric.add(hdlSampleLengthGeneric)
    entity.getGeneric.add(hdlPadLengthGeneric)
    entity.getPort.add(hdlClkSgnl)
    entity.getPort.add(hdlRstSgnl)


    entity.getPort.add(hdlEnSgnl)
    entity.getPort.add(hdlIinSgnl)
    entity.getPort.add(hdlQinSgnl)
    entity.getPort.add(hdlVldSgnl)
    entity.getPort.add(hdlIoutSgnl)
    entity.getPort.add(hdlQoutSgnl)

    entity
  }

  /**
    * Implement the counter, declare internal signals etc.
    *
    * @return the architecture containing the template implementation
    */
  override  def getHdlArchitecture: Architecture = {
    // the architecture requires the associated entity
    //architecture =

    // generation of internal register signals
    val hdlPadCntSgnl: Signal = new Signal("pad_cnt", Standard.INTEGER, new DecimalLiteral(0))
    architecture.getDeclarations.add(new SignalDeclaration(hdlPadCntSgnl))
    val hdlSampleCntSgnl: Signal = new Signal("sample_cnt", Standard.INTEGER, new DecimalLiteral(0))
    architecture.getDeclarations.add(new SignalDeclaration(hdlSampleCntSgnl))
    val hdlStateSgnl = new Signal("state", StdLogic1164.STD_LOGIC_VECTOR(2), Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0))
    architecture.getDeclarations.add(new SignalDeclaration(hdlStateSgnl))

    // a process to determine the next register contents
    val hdlProc: ProcessStatement = new ProcessStatement("zeropad_proc")

    // the process contains an if block
    val hdlProcIf: IfStatement = new IfStatement(new Equals(hdlRstSgnl, StdLogic1164.STD_LOGIC_1))
    hdlProcIf.getStatements.add(new SignalAssignment(hdlStateSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
    hdlProcIf.getStatements.add(new SignalAssignment(hdlIoutSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
    hdlProcIf.getStatements.add(new SignalAssignment(hdlQoutSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
    hdlProcIf.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_0))
    hdlProcIf.getStatements.add(new SignalAssignment(hdlPadCntSgnl, new DecimalLiteral(0)))
    hdlProcIf.getStatements.add(new SignalAssignment(hdlSampleCntSgnl, new DecimalLiteral(0)))

    hdlProcIf.createElsifPart(new And(new AttributeExpression[Signal](hdlClkSgnl,
      new Attribute("EVENT", null)), new Equals(hdlClkSgnl, StdLogic1164.STD_LOGIC_1)))
    hdlProcIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(hdlIoutSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
    hdlProcIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(hdlQoutSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
    hdlProcIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_0))
    hdlProcIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(hdlPadCntSgnl, new DecimalLiteral(0)))
    hdlProcIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(hdlSampleCntSgnl, new DecimalLiteral(0)))

    // FSM States
    val hdlFsm = new CaseStatement(hdlStateSgnl)
    val fsms = for (i <- 0 to 3)
      yield hdlFsm.createAlternative(new BinaryLiteral(i.toBinaryString.reverse.padTo(2, "0").reverse.mkString))


    var stateCount = 0
    for (hdlState <- fsms) {
      if (stateCount == 0) { // First State
        hdlState.getStatements.add(new SignalAssignment(hdlStateSgnl, new BinaryLiteral("00")))

        val hdlEnIf: IfStatement = new IfStatement(new Equals(hdlEnSgnl, StdLogic1164.STD_LOGIC_1))
        hdlEnIf.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_1))
        hdlEnIf.getStatements.add(new SignalAssignment(hdlIoutSgnl, hdlIinSgnl))
        hdlEnIf.getStatements.add(new SignalAssignment(hdlQoutSgnl, hdlQinSgnl))
        hdlEnIf.getStatements.add(new SignalAssignment(hdlSampleCntSgnl, new Add(hdlSampleCntSgnl, new DecimalLiteral(1))))
        hdlState.getStatements.add(hdlEnIf)
      } else if (stateCount == 1) {
        hdlState.getStatements.add(new SignalAssignment(hdlStateSgnl, new BinaryLiteral("01")))
        hdlState.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_1))
        hdlState.getStatements.add(new SignalAssignment(hdlIoutSgnl, hdlIinSgnl))
        hdlState.getStatements.add(new SignalAssignment(hdlQoutSgnl, hdlQinSgnl))
        hdlState.getStatements.add(new SignalAssignment(hdlSampleCntSgnl, new Add(hdlSampleCntSgnl, new DecimalLiteral(1))))

        val hdlState1If: IfStatement = new IfStatement(new Equals(hdlSampleCntSgnl, hdlSampleLengthGeneric))
        hdlState1If.getStatements.add(new SignalAssignment(hdlStateSgnl, new BinaryLiteral("10")))
        hdlState.getStatements.add(hdlState1If)
      } else if (stateCount == 2) {
        hdlState.getStatements.add(new SignalAssignment(hdlStateSgnl, new BinaryLiteral("10")))
        hdlState.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_1))
        hdlState.getStatements.add(new SignalAssignment(hdlIoutSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
        hdlState.getStatements.add(new SignalAssignment(hdlQoutSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
        hdlState.getStatements.add(new SignalAssignment(hdlSampleCntSgnl, new Add(hdlSampleCntSgnl, new DecimalLiteral(1))))

        val hdlState2If: IfStatement = new IfStatement(new Equals(hdlSampleCntSgnl, new Add(hdlPadLengthGeneric, hdlSampleLengthGeneric)))
        hdlState2If.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_0))
        hdlState2If.getStatements.add(new SignalAssignment(hdlSampleCntSgnl, new DecimalLiteral(0)))
        hdlState2If.getStatements.add(new SignalAssignment(hdlStateSgnl, new BinaryLiteral("11")))
        hdlState.getStatements.add(hdlState2If)
      }else if (stateCount == 3) {
        hdlState.getStatements.add(new SignalAssignment(hdlStateSgnl, new BinaryLiteral("00")))
        hdlState.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_0))
        hdlState.getStatements.add(new SignalAssignment(hdlIoutSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
        hdlState.getStatements.add(new SignalAssignment(hdlQoutSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
      }
      stateCount = stateCount + 1
    }

    val stateX = hdlFsm.createAlternative(new BasedLiteral("others"))
    stateX.getStatements.add(new SignalAssignment(hdlStateSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
    stateX.getStatements.add(new SignalAssignment(hdlStateSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
    stateX.getStatements.add(new SignalAssignment(hdlIoutSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
    stateX.getStatements.add(new SignalAssignment(hdlQoutSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
    stateX.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_0))
    stateX.getStatements.add(new SignalAssignment(hdlPadCntSgnl, new DecimalLiteral(0)))
    stateX.getStatements.add(new SignalAssignment(hdlSampleCntSgnl, new DecimalLiteral(0)))

    hdlProcIf.getElsifParts.get(0).getStatements.add(hdlFsm)
    hdlProc.getStatements.add(hdlProcIf)

    // modify the sensitivity list
    hdlProc.getSensitivityList.add(hdlClkSgnl)
    hdlProc.getSensitivityList.add(hdlRstSgnl)

    // add the process to the architecture
    architecture.getStatements.add(hdlProc)

    architecture
  }
}
