package sdrlift.graph

import sdrlift.codegen.vhdl.VhdlTemplateCodeGen
import exp.CompExp.{Combinational, Component}
import exp.NodeExp.{Exp, Streamer}
import sdrlift.codegen.vhdl.{VhdlComponentCodeGen, VhdlKernelCodeGen}
import sdrlift.graph.NodeFactory.PortTypeEnum.PortTypeEnum
import scala.util.Random

object NodeFactory {

  abstract class Node extends DfgNode {
    override val name: String
  }

  //------------------------- Fine-grained Nodes --------------------------

  case class StrmNode(stm: Streamer) extends Node {
    override val inst: String = stm.name
    override val name: String = stm.name
    override val prefix: String = "st"
    override val width: Int = stm.width
    override val level: Int = stm.lvl
    override val typ: PortTypeEnum = stm.typ
    override val operator = null
  }

  case class ConstNode(vl: Int, lvl: Int) extends Node {
    override val inst: String = s"ct_${vl}"
    override val name: String = s"ct_${vl}"
    override val prefix: String = "ct"
    override val width: Int = 0
    override val level: Int = lvl
    override val operator = null
    override val typ: PortTypeEnum = null
  }

  case class ArithNode(name: String, lhs: Exp, rhs: Exp, nodeWidth: Int, opr: String, lvl: Int)
    extends Node {
    override val inst: String = arithLogiInst(name, opr)
    override val prefix: String = "arith"
    override val width: Int = nodeWidth
    override val level: Int = lvl
    override val operator = opr
    override val typ: PortTypeEnum = null
  }

  case class LogiNode(name: String, lhs: Exp, rhs: Exp, nodeWidth: Int, opr: String, lvl: Int)
    extends Node {
    override val inst: String = arithLogiInst(name, opr)
    override val prefix: String = "lgc"
    override val width: Int = nodeWidth
    override val level: Int = lvl
    override val operator = opr
    override val typ: PortTypeEnum = null
  }

  case class CondNode(name: String, prfx: String, lvl: Int)
    extends Node {
    override val inst: String = name
    override val prefix: String = "cnd"
    override val width: Int = 0
    override val level: Int = lvl
    override val operator = null
    override val typ: PortTypeEnum = null
  }

  // Directive Nodes
  case class DirNode(name: String, ctrl: String)
    extends Node {
    val prfx = if(ctrl.equals("Combinational")) "cmb" else "seq"
    override val inst: String = if (!name.startsWith("cmb_")) s"cmb_${name}" else name
    override val prefix: String = "cmb"
    override val width: Int = 0
    override val level: Int = 0
    override val operator = null
    override val typ: PortTypeEnum = null
  }

  // Control Nodes
  abstract class CtrlNode extends Node

  // FSM
  case class FsmNode(name: String, firstState: Int, lvl: Int)
    extends CtrlNode {
    override val inst: String = if (!name.startsWith("fsm_")) s"fsm_${name}" else name
    //override val name: String = if (!id.startsWith("fsm_")) s"fsm_${id}" else id
    override val prefix: String = "fsm"
    override val width: Int = 0
    override val level: Int = lvl
    override val operator = null
    override val typ: PortTypeEnum = null
  }

  case class StateNode(name: String, lvl: Int)
    extends CtrlNode {
    override val inst: String = name
    // override val name: String = id
    override val prefix: String = "sx"
    override val width: Int = 0
    override val level: Int = lvl
    override val operator = null
    override val typ: PortTypeEnum = null
  }

  // IF-Else Statement
  case class IfNode(name: String, lvl: Int)
    extends CtrlNode {
    override val inst: String = if (!name.startsWith("if_")) s"if_${name}" else name
    //override val name: String = if (!id.startsWith("if_")) s"if_${id}" else id
    override val prefix: String = "if"
    override val width: Int = 0
    override val level: Int = lvl
    override val operator = null
    override val typ: PortTypeEnum = null
  }

  case class ElsIfNode(name: String, lvl: Int)
    extends CtrlNode {
    override val inst: String = if (!name.startsWith("elsif_")) s"elsif_${name}" else name
    // override val name: String = if (!id.startsWith("elsif_")) s"elsif_${id}" else id
    override val prefix: String = "elsif"
    override val width: Int = 0
    override val level: Int = lvl
    override val operator = null
    override val typ: PortTypeEnum = null
  }

  case class ElsNode(name: String, lvl: Int)
    extends CtrlNode {
    override val inst: String = if (!name.startsWith("els_")) s"els_${name}" else name
    // override val name: String = if (!name.startsWith("els_")) s"els_${name}" else name
    override val prefix: String = "els"
    override val width: Int = 0
    override val level: Int = lvl
    override val operator = null
    override val typ: PortTypeEnum = null
  }


  //------------------------- Coarse-grained Nodes --------------------------
  // Component Nodes
  case class CompNode(comp: Component) extends Node {
    override val inst: String = comp.inst
    override val name: String = comp.name
    override val prefix: String = "cmp"
    override val width: Int = comp.width
    override val level: Int = 0
    override val operator = null
    override val typ: PortTypeEnum = null

    def vhdlfile = VhdlComponentCodeGen(comp).getHdlFile()
  }

  case class TmplNode(comp: Component)
    extends Node {
    override val inst: String = comp.inst
    override val name: String = comp.name
    override val prefix: String = "tmpl"
    override val width: Int = comp.width
    override val level: Int = 0
    override val operator = null
    override val typ: PortTypeEnum = null

    def vhdlfile = VhdlTemplateCodeGen(name, comp._params).getVhdlCodeGen.getHdlFile()
  }

  //------------------------- General Methods --------------------------

  object PortTypeEnum extends Enumeration {
    type PortTypeEnum = Value
    val CLK, RST, EN, DIN, VLD, DOUT, Z = Value
  }

  def randomStr(n: Int): String = {
    val alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val size = alpha.size
    (1 to n).map(_ => alpha(Random.nextInt(alpha.length))).mkString
  }

  def shortHash(sha: String) ={
    val hash = Integer.toString(sha.hashCode & 0x7FFFFFFF, 36)
    if(hash.length > 6) hash.substring(0, 6) else hash
  }

  def arithLogiInst(name: String, prfx: String) = {
   // println(Integer.toString(name.hashCode & 0x7FFFFFFF, 36))
    if (name.length > 24) prfx + "_" + shortHash(name) else name
  }

  object AsInt {
    def unapply(s: String) = try {
      Some(s.toInt)
    } catch {
      case e: NumberFormatException => None
    }
  }

}



