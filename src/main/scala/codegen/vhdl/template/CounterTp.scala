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

case class CounterTp(params : Map[String,Any]) extends VhdlCodeGen {
  val width: Int = params.get("width").get.asInstanceOf[Int]
  val inst = params.get("inst").get

  private val file: VhdlFile = new VhdlFile
  private val entity: Entity = new Entity(inst + "_counter")
  private val architecture: Architecture = new Architecture("rtl", entity)

  private val hdlWidthGeneric: Constant = new Constant("WIDTH", Standard.POSITIVE, new DecimalLiteral(width))

  private val hdlClkSgnl: Signal = new Signal("clk", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlRstSgnl: Signal = new Signal("rst", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlEnSgnl: Signal = new Signal("en", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlDoutSgnl: Signal = new Signal("dout", Mode.OUT, StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric));

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
    entity.getPort.add(hdlClkSgnl)
    entity.getPort.add(hdlRstSgnl)


    entity.getPort.add(hdlEnSgnl)
    entity.getPort.add(hdlDoutSgnl)

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
    val hdlProc: ProcessStatement = new ProcessStatement("count_proc")

    // generation of internal register signals
    val hdlCountSgnl = new Signal("count", StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric), Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0))
    architecture.getDeclarations.add(new SignalDeclaration(hdlCountSgnl))


    // the process contains an if block
    val hdlProcIf: IfStatement = new IfStatement(new Equals(hdlRstSgnl, StdLogic1164.STD_LOGIC_1))
    hdlProcIf.getStatements.add(new SignalAssignment(hdlCountSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))

    hdlProcIf.createElsifPart(new And(new AttributeExpression[Signal](hdlClkSgnl,
      new Attribute("EVENT", null)), new Equals(hdlClkSgnl, StdLogic1164.STD_LOGIC_1)))
    hdlProcIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(hdlCountSgnl, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))

    val hdlIf: IfStatement = new IfStatement(new Equals(hdlEnSgnl, StdLogic1164.STD_LOGIC_1))
    hdlIf.getStatements.add(new SignalAssignment(hdlCountSgnl, new Add(hdlCountSgnl, new DecimalLiteral(1))))
    hdlProcIf.getElsifParts.get(0).getStatements.add(hdlIf)

    hdlProc.getStatements.add(hdlProcIf)

    //  architecture.getStatements.add(new ConditionalSignalAssignment(hdlDoutSgnl,
    //    hdlMemVrbl.getArrayElement(new Subtract(hdlDepthGeneric, new DecimalLiteral(1)))))

    // modify the sensitivity list
    hdlProc.getSensitivityList.add(hdlClkSgnl)
    hdlProc.getSensitivityList.add(hdlRstSgnl)

    architecture.getStatements.add(new ConditionalSignalAssignment(hdlDoutSgnl,  hdlCountSgnl))

    // add the process to the architecture
    architecture.getStatements.add(hdlProc)

    // forward the register output to the entity output
    // architecture.getStatements.add(new ConditionalSignalAssignment(output, count))

    architecture
  }
}
