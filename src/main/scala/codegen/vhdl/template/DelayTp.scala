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
import de.upb.hni.vmagic.statement.{ForStatement, IfStatement, SignalAssignment}
import de.upb.hni.vmagic.`type`.ConstrainedArray
import de.upb.hni.vmagic.`type`.SubtypeIndication
import de.upb.hni.vmagic.`type`.UnconstrainedArray
import de.upb.hni.vmagic._
import de.upb.hni.vmagic.Range
import de.upb.hni.vmagic.`object`.VhdlObject.Mode
import de.upb.hni.vmagic.builtin.StdLogicSigned
import de.upb.hni.vmagic.builtin.StdLogicUnsigned
import de.upb.hni.vmagic.builtin.Standard
import de.upb.hni.vmagic.literal.{DecimalLiteral, StringLiteral}

case class DelayTp(params : Map[String,Any]) extends VhdlCodeGen{
  val width: Int = params.get("width").get.asInstanceOf[Int]
  val depth: Int = params.get("depth").get.asInstanceOf[Int]

  private val file: VhdlFile = new VhdlFile
  private val entity: Entity = new Entity("delay")
  private val architecture: Architecture = new Architecture("rtl", entity)

  private val hdlWidthGeneric: Constant = new Constant("WIDTH", Standard.POSITIVE, new DecimalLiteral(width))
  private val hdlDepthGeneric: Constant = new Constant("DEPTH", Standard.POSITIVE, new DecimalLiteral(depth))
  private val hdlClkSgnl: Signal = new Signal("clk", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlRstSgnl: Signal = new Signal("rst", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlEnSgnl: Signal = new Signal("en", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlDinSgnl: Signal = new Signal("din", Mode.IN, StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric));
  private val hdlVldSgnl: Signal = new Signal("vld", Mode.OUT, StdLogic1164.STD_LOGIC)
  private val hdlDoutSgnl: Signal = new Signal("dout", Mode.OUT, StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric));

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
    entity.getGeneric.add(hdlDepthGeneric)
    entity.getPort.add(hdlClkSgnl)
    entity.getPort.add(hdlRstSgnl)


    entity.getPort.add(hdlEnSgnl)
    entity.getPort.add(hdlDinSgnl)
    entity.getPort.add(hdlVldSgnl)
    entity.getPort.add(hdlDoutSgnl)

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
    val hdlDlyType = new ConstrainedArray("dly_type", StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric),
      new Range(new DecimalLiteral(0), Range.Direction.TO,
        new Subtract(hdlDepthGeneric, new DecimalLiteral(1))));
    architecture.getDeclarations.add(hdlDlyType)
    val hdlDlySgnl: Signal = new Signal("dly", hdlDlyType,
      Aggregate.OTHERS(Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
    architecture.getDeclarations.add(new SignalDeclaration(hdlDlySgnl))

    // a process to determine the next register contents
    val hdlProc: ProcessStatement = new ProcessStatement("dly_proc")

    // the process contains an if block
    val hdlIf: IfStatement = new IfStatement(new Equals(hdlRstSgnl, StdLogic1164.STD_LOGIC_1))
    hdlIf.getStatements.add(new SignalAssignment(hdlDlySgnl,
      Aggregate.OTHERS(Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0))))
    hdlIf.createElsifPart(new And(new AttributeExpression[Signal](hdlClkSgnl,
      new Attribute("EVENT", null)), new Equals(hdlClkSgnl, StdLogic1164.STD_LOGIC_1)))

    hdlIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_0))
    hdlIf.getElsifParts.get(0).getStatements.
      add(new SignalAssignment(hdlDlySgnl,  Aggregate.OTHERS(Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0))))

    val hdlIf1: IfStatement = new IfStatement(new Equals(hdlEnSgnl, StdLogic1164.STD_LOGIC_1))
    hdlIf1.getStatements.add(new SignalAssignment(hdlVldSgnl, StdLogic1164.STD_LOGIC_1))
    hdlIf1.getStatements.add(new SignalAssignment(hdlDlySgnl.getArrayElement(new DecimalLiteral(0)), hdlDinSgnl))

    val hdlIf2: IfStatement = new IfStatement(new GreaterThan(hdlDepthGeneric, new DecimalLiteral(1)))

    val hdlForRange = new Range(new Subtract(hdlDepthGeneric, new DecimalLiteral(2)),
      Range.Direction.DOWNTO, new DecimalLiteral(0))
    val hdlFor = new ForStatement("i", hdlForRange)
    hdlFor.getStatements.add(new SignalAssignment(
      hdlDlySgnl.getArrayElement(new Add(new Signal("i", StdLogic1164.STD_LOGIC), new DecimalLiteral(1))),
      hdlDlySgnl.getArrayElement(new Signal("i", StdLogic1164.STD_LOGIC))))
    hdlIf2.getStatements.add(hdlFor)

    hdlIf.getElsifParts.get(0).getStatements.add(hdlIf1)
    hdlProc.getStatements.add(hdlIf)

    hdlIf1.getStatements.add(hdlIf2)

    architecture.getStatements.add(new ConditionalSignalAssignment(hdlDoutSgnl,
      hdlDlySgnl.getArrayElement(new Subtract(hdlDepthGeneric, new DecimalLiteral(1)))))

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
