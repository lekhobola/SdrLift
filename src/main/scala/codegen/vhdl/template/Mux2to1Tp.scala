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
import de.upb.hni.vmagic.concurrent.ConditionalSignalAssignment.ConditionalWaveformElement
import de.upb.hni.vmagic.literal.{DecimalLiteral, StringLiteral}
import scala.collection.JavaConverters._

case class Mux2to1Tp(params : Map[String,Any]) extends VhdlCodeGen {
  val width: Int = params.get("width").get.asInstanceOf[Int]

  private val file: VhdlFile = new VhdlFile
  private val entity: Entity = new Entity("mux_2_to_1")
  private val architecture: Architecture = new Architecture("rtl", entity)

  private val hdlWidthGeneric: Constant = new Constant("WIDTH", Standard.POSITIVE, new DecimalLiteral(width))
  private val hdlSelSgnl: Signal = new Signal("sel", Mode.IN, StdLogic1164.STD_LOGIC)
  private val hdlDin1Sgnl: Signal = new Signal("din1", Mode.IN, StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric));
  private val hdlDin2Sgnl: Signal = new Signal("din2", Mode.IN, StdLogic1164.STD_LOGIC_VECTOR(hdlWidthGeneric));
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

    entity.getPort.add(hdlSelSgnl)
    entity.getPort.add(hdlDin1Sgnl)
    entity.getPort.add(hdlDin2Sgnl)
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
    val hdlWaveformEls1 = List(new WaveformElement(hdlDin1Sgnl))
    val hdlCWEls1 = new ConditionalSignalAssignment.ConditionalWaveformElement(hdlWaveformEls1.asJava, new Equals(hdlSelSgnl, StdLogic1164.STD_LOGIC_0))

    val hdlWaveformEls2 = List(new WaveformElement(hdlDin2Sgnl))
    val hdlCWEls2 = new ConditionalSignalAssignment.ConditionalWaveformElement(hdlWaveformEls2.asJava)

    architecture.getStatements.add(new ConditionalSignalAssignment(hdlDoutSgnl, List(hdlCWEls1, hdlCWEls2).asJava))

    architecture
  }
}
