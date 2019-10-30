package sdrlift.codegen.vhdl.template

import sdrlift.codegen.vhdl.VhdlCodeGen
import de.upb.hni.vmagic.VhdlFile
import de.upb.hni.vmagic.builtin._
import de.upb.hni.vmagic.concurrent.ConditionalSignalAssignment
import de.upb.hni.vmagic.concurrent.ProcessStatement
import de.upb.hni.vmagic.declaration.{Attribute, ConstantDeclaration, SignalDeclaration, VariableDeclaration}
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
import de.upb.hni.vmagic.concurrent.ConditionalSignalAssignment.ConditionalWaveformElement
import de.upb.hni.vmagic.literal.{BasedLiteral, DecimalLiteral, PhysicalLiteral, StringLiteral}

import scala.collection.JavaConverters._

case class RounderTp(params : Map[String,Any]) extends VhdlCodeGen {
  val dinWidth: Int = params.get("din_width").get.asInstanceOf[Int]
  val doutWidth: Int = params.get("dout_width").get.asInstanceOf[Int]
  val inst = params.get("inst").get

  private val file: VhdlFile = new VhdlFile
  private val entity: Entity = new Entity(inst + "_rounder")
  private val architecture: Architecture = new Architecture("rtl", entity)

  private val hdlDinWidthGeneric: Constant = new Constant("DIN_WIDTH", Standard.POSITIVE, new DecimalLiteral(dinWidth))
  private val hdlDoutWidthGeneric: Constant = new Constant("DOUT_WIDTH", Standard.POSITIVE, new DecimalLiteral(doutWidth))
  private val hdlDinSgnl: Signal = new Signal("din", Mode.IN, StdLogic1164.STD_LOGIC_VECTOR(hdlDinWidthGeneric))
  private val hdlDoutSgnl: Signal = new Signal("dout", Mode.OUT, StdLogic1164.STD_LOGIC_VECTOR(hdlDoutWidthGeneric));

  private def aggregateRangeWithBit0(high: String, low: String) = {
    new BasedLiteral("(" + high + " - 1 downto " + low + " => '0')")
  }

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
    file.getElements.add(NumericStd.USE_CLAUSE)
    file.getElements.add(StdLogicSigned.USE_CLAUSE)

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
    entity.getGeneric.add(hdlDinWidthGeneric)
    entity.getGeneric.add(hdlDoutWidthGeneric)

    entity.getPort.add(hdlDinSgnl)
    entity.getPort.add(hdlDoutSgnl)

    entity
  }

  /**
    * Implement the counter, declare internal signals etc.
    *
    * @return the architecture containing the template implementation
    */
  override def getHdlArchitecture: Architecture = {

    // add the process to the architecture
    // generation of internal register signals
    val hdlRadPosConst = new Constant("rad_pos", Standard.INTEGER, new Subtract(hdlDinWidthGeneric, hdlDoutWidthGeneric))
    architecture.getDeclarations.add(new ConstantDeclaration(hdlRadPosConst))
    val hdlRoundedRegSgnl = new Signal("rounded_reg", StdLogic1164.STD_LOGIC_VECTOR(hdlDoutWidthGeneric), Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0))
    architecture.getDeclarations.add(new SignalDeclaration(hdlRoundedRegSgnl))

    // a process to determine the next register contents
    val hdlProc: ProcessStatement = new ProcessStatement("rounder_proc")

    // Variables
    val hdlRoundTempVrbl = new Variable("rounded_temp", StdLogic1164.STD_LOGIC_VECTOR(new Add(hdlDoutWidthGeneric, new DecimalLiteral(1))))
    hdlProc.getDeclarations.add(new VariableDeclaration(hdlRoundTempVrbl))

    // the process contains an if block
    val hdlIf: IfStatement = new IfStatement(new GreaterThan(hdlRadPosConst, new DecimalLiteral(0)))

    val hdlfc1 = new FunctionCall(StdLogicUnsigned.CONV_INTEGER_SLV_INTEGER)
    hdlfc1.getParameters.add(new AssociationElement(hdlDinSgnl.getSlice(new Range(new Subtract(hdlDinWidthGeneric, new DecimalLiteral(1)), Range.Direction.DOWNTO, hdlRadPosConst))))
    val hldPwr = new Subtract(new Parentheses(new Pow(new DecimalLiteral(2), hdlDoutWidthGeneric)), new DecimalLiteral(1))

    val hdlIfIf: IfStatement = new IfStatement(new Equals(hdlfc1, hldPwr))
    val hdlExtendedDinSgnl = new Concatenate(hdlDinSgnl.getArrayElement(new Subtract(hdlDinWidthGeneric, new DecimalLiteral(1))), hdlDinSgnl.getSlice(new Range(new Subtract(hdlDinWidthGeneric, new DecimalLiteral(1)), Range.Direction.DOWNTO, hdlRadPosConst)))
    val hdlIfIfSa1 = new VariableAssignment(hdlRoundTempVrbl, hdlExtendedDinSgnl)
    hdlIfIf.getStatements.add(hdlIfIfSa1)

    val hdlIfIfElseIf: IfStatement = new IfStatement(new Equals(hdlRadPosConst, new DecimalLiteral(1)))
    val hdlfc2 = new FunctionCall(StdLogicUnsigned.CONV_INTEGER_SLV_INTEGER)
    hdlfc2.getParameters.add(new AssociationElement(hdlDinSgnl))
    val hdlfc3 = new FunctionCall(StdLogicUnsigned.CONV_INTEGER_SLV_INTEGER)
    hdlfc3.getParameters.add(new AssociationElement(hdlDinSgnl.getSlice(new Range(new Subtract(hdlRadPosConst, new DecimalLiteral(1)), Range.Direction.DOWNTO, new DecimalLiteral(0)))))
    hdlIfIfElseIf.getStatements.add(new VariableAssignment(hdlRoundTempVrbl, new Add(hdlExtendedDinSgnl, new Parentheses(new Concatenate(aggregateRangeWithBit0(hdlDinWidthGeneric.getIdentifier, hdlRadPosConst.getIdentifier), hdlDinSgnl.getArrayElement(0))))))

    val hdlIfIfElseIfElseIf: IfStatement = new IfStatement(new And(new LessThan(hdlfc2, new DecimalLiteral(0)), new Equals(hdlfc3, new DecimalLiteral(0))))
    hdlIfIfElseIfElseIf.getStatements.add(new VariableAssignment(hdlRoundTempVrbl, hdlExtendedDinSgnl))
    hdlIfIfElseIfElseIf.getElseStatements.add(new VariableAssignment(hdlRoundTempVrbl, new Add(hdlExtendedDinSgnl, new Parentheses(new Concatenate(aggregateRangeWithBit0(hdlDinWidthGeneric.getIdentifier, hdlRadPosConst.getIdentifier), hdlDinSgnl.getArrayElement(new Subtract(hdlRadPosConst, new DecimalLiteral(1))))))))

    hdlIfIfElseIf.getElseStatements.add(hdlIfIfElseIfElseIf)

    hdlIfIf.getElseStatements.add(hdlIfIfElseIf)
    hdlIf.getStatements.add(hdlIfIf)
    hdlIf.getStatements.add(new SignalAssignment(hdlRoundedRegSgnl, hdlRoundTempVrbl.getSlice(new Range(new Subtract(hdlDoutWidthGeneric, new DecimalLiteral(1)), Range.Direction.DOWNTO, new DecimalLiteral(0)))))
    hdlIf.getElseStatements.add(new SignalAssignment(hdlRoundedRegSgnl, hdlRoundTempVrbl.getSlice(new Range(new Subtract(hdlDinWidthGeneric, new DecimalLiteral(1)), Range.Direction.DOWNTO, hdlRadPosConst))))

    hdlProc.getStatements.add(hdlIf)
    hdlProc.getSensitivityList.add(hdlDinSgnl)

    architecture.getStatements.add(hdlProc)
    architecture.getStatements.add(new ConditionalSignalAssignment(hdlDoutSgnl, hdlRoundedRegSgnl))

    architecture
  }
}
