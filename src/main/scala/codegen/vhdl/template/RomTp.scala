package sdrlift.codegen.vhdl.template

import sdrlift.codegen.vhdl.VhdlCodeGen
import de.upb.hni.vmagic.VhdlFile
import de.upb.hni.vmagic.builtin.StdLogic1164
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
import de.upb.hni.vmagic.builtin.StdLogicSigned
import de.upb.hni.vmagic.builtin.StdLogicUnsigned
import de.upb.hni.vmagic.builtin.Standard
import de.upb.hni.vmagic.concurrent.ConditionalSignalAssignment.ConditionalWaveformElement
import de.upb.hni.vmagic.literal.{DecimalLiteral, PhysicalLiteral, StringLiteral}

import scala.collection.JavaConverters._

case class RomTp(params : Map[String,Any]) extends VhdlCodeGen {
  val addrWidth: Int = params.get("addr_width").get.asInstanceOf[Int]
  val dataWidth: Int = params.get("data_width").get.asInstanceOf[Int]
  val vector: Seq[Int] = params.get("vector").get.asInstanceOf[Seq[Int]]
  val inst = params.get("inst").get

  private val file: VhdlFile = new VhdlFile
  private val entity: Entity = new Entity(inst + "_rom")
  private val architecture: Architecture = new Architecture("rtl", entity)

  private val hdlAddrWidthGeneric: Constant = new Constant("ADDR_WIDTH", Standard.POSITIVE, new DecimalLiteral(addrWidth))
  private val hdlDataDepthGeneric: Constant = new Constant("DATA_WIDTH", Standard.POSITIVE, new DecimalLiteral(dataWidth))
  private val hdlAddrSgnl: Signal = new Signal("addr", Mode.IN, StdLogic1164.STD_LOGIC_VECTOR(hdlAddrWidthGeneric))
  private val hdlDoutSgnl: Signal = new Signal("dout", Mode.OUT, StdLogic1164.STD_LOGIC_VECTOR(hdlDataDepthGeneric));

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
    entity.getGeneric.add(hdlAddrWidthGeneric)
    entity.getGeneric.add(hdlDataDepthGeneric)

    entity.getPort.add(hdlAddrSgnl)
    entity.getPort.add(hdlDoutSgnl)

    entity
  }

  /**
    * Implement the counter, declare internal signals etc.
    *
    * @return the architecture containing the template implementation
    */
  override def getHdlArchitecture: Architecture = {


    def hdlArrayInit(seq: Seq[Int], width: Int) = {
      var str = "("
      for (i <- 0 until seq.length) {
        val bin = seq(i).toBinaryString
        val binVal = if(bin.length > width) bin.substring(bin.length - width, bin.length) else bin.reverse.padTo((width), '0').reverse
        str = str + "\"" + binVal + "\""
        if (i < seq.length - 1) str = str + ", " else str = str + ")"
      }
      str
    }
    // add the process to the architecture
    // generation of internal register signals
    val hdlMemType = new ConstrainedArray("mem_type", StdLogic1164.STD_LOGIC_VECTOR(hdlDataDepthGeneric),
      new Range(new DecimalLiteral(0), Range.Direction.TO,
        new Subtract(new Parentheses(new Pow(new DecimalLiteral(2), hdlAddrWidthGeneric)), new DecimalLiteral(1))))
    architecture.getDeclarations.add(hdlMemType)
    val hdlMemConst = new Constant("memory", hdlMemType,
      new PhysicalLiteral(hdlArrayInit(vector, dataWidth)))
    architecture.getDeclarations.add(new ConstantDeclaration(hdlMemConst))

    val hdlfc = new FunctionCall(StdLogicUnsigned.CONV_INTEGER_SLV_INTEGER)
    hdlfc.getParameters.add(new AssociationElement(hdlAddrSgnl))
    architecture.getStatements.add(new ConditionalSignalAssignment(hdlDoutSgnl, new TypeConversion(StdLogic1164.STD_LOGIC_VECTOR, hdlMemConst.getArrayElement(hdlfc))))

    architecture
  }
}
