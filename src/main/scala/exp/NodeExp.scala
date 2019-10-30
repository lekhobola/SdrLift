package exp

import exp.CompExp.{Comp, Component, ConstVal, constName}
import scalax.collection.Graph
import scalax.collection.edge.Implicits._
import sdrlift.graph.Dfg._
import sdrlift.graph.{DfgEdge, DfgNode}
import sdrlift.graph.DfgNode
import sdrlift.graph.NodeFactory.PortTypeEnum.PortTypeEnum
import sdrlift.graph.NodeFactory._

import scala.collection.Seq

object NodeExp {

  def getNodeExp(n: DfgNode): Exp = n match {
    //Node Factory
    case strmNode: StrmNode => strmNode.stm
    case cnst: ConstNode => Const(cnst.vl, 0, cnst.width)
    case arithNode: ArithNode => Arith2Arg(arithNode.lhs, arithNode.rhs, arithNode.opr)
    case lgcNode: LogiNode =>  Logi2Arg(lgcNode.lhs, lgcNode.rhs, lgcNode.opr)
    case cmpNode: CompNode => cmpNode.comp
    case tmplNode: TmplNode => tmplNode.comp
    case _ => null
  }

  abstract class Exp {
    val level: Int = 0
    val width: Int = 0

    def getLastNodeId = {
      if (this.isInstanceOf[Arith2Arg] || this.isInstanceOf[Logi2Arg]) {
        case class DummyComp(inst: String) extends Component {
          override val name: String = inst
          override val width: Int = 0

          override def dfg: Graph[DfgNode, DfgEdge] = model(Seq(this))
        }
        val cmp = DummyComp("dummy")
        val g = cmp.model(Seq(this))
        g.getLeaves.head.inst
      } else if (this.isInstanceOf[Component]) {
        this.asInstanceOf[Component].inst
      } else {
        this match {
          case strm: Streamer => strm.name
          case cnst: Const => "ct_" + cnst.lvl
        }
      }
    }

  }
  //--------------------------------------------------------------------------------------------------------------------
  // Basic Operations
  //--------------------------------------------------------------------------------------------------------------------
  object Link {
    def unapply(arg: Link2Arg): Option[(Any, Any)] = {
      Some(arg.src, arg.tgt)
    }

    def apply(lhs: Streamer, rhs: Streamer) = new Link2Arg(lhs, rhs)
  }

  //--------------------------------------------------------------------------------------------------------------------

  // Arithmetic Primitives
  //--------------------------------------------------------------------------------------------------------------------

  /*case class Add(override val opt1: Node, override val opt2: Node) extends Arith2arg.lhs, opt2) {
  override val prefix: String = "add"
  override val name: String = if(opt1.name != null || opt2.name != null ) s"add_${opt1.name}_${opt2.name}" else null
}*/
  object Add {
    def unapply(arg: Arith2Arg): Option[(Any, Any)] = {
      Some(arg.lhs, arg.rhs)
    }

    val prefix: String = "add"

    def apply(lhs: Exp, rhs: Exp) = new Arith2Arg(lhs, rhs, prefix)
  }

  object Sub {
    def unapply(arg: Arith2Arg): Option[(Any, Any)] = {
      Some(arg.lhs, arg.rhs)
    }

    val prefix: String = "sub"

    def apply(lhs: Exp, rhs: Exp) = new Arith2Arg(lhs, rhs, prefix)
  }

  object Mul {
    def unapply(arg: Arith2Arg): Option[(Any, Any)] = {
      Some(arg.lhs, arg.rhs)
    }

    val prefix: String = "mul"

    def apply(lhs: Exp, rhs: Exp) = new Arith2Arg(lhs, rhs, prefix)
  }

  object Div {
    def unapply(arg: Arith2Arg): Option[(Any, Any)] = {
      Some(arg.lhs, arg.rhs)
    }

    val prefix: String = "div"

    def apply(lhs: Exp, rhs: Exp) = new Arith2Arg(lhs, rhs, prefix)
  }

  //--------------------------------------------------------------------------------------------------------------------

  // Logical Comparisons
  //--------------------------------------------------------------------------------------------------------------------
  object Le {
    def unapply(arg: Logi2Arg): Option[(Any, Any)] = {
      Some(arg.lhs, arg.rhs)
    }

    val prefix: String = "le"

    def apply(lhs: Exp, rhs: Exp) = new Logi2Arg(lhs, rhs, prefix)
  }

  object And {
    def unapply(arg: Logi2Arg): Option[(Any, Any)] = {
      Some(arg.lhs, arg.rhs)
    }

    val prefix: String = "and"

    def apply(lhs: Exp, rhs: Exp) = new Logi2Arg(lhs, rhs, prefix)
  }

  object Or {
    def unapply(arg: Logi2Arg): Option[(Any, Any)] = {
      Some(arg.lhs, arg.rhs)
    }

    val prefix: String = "or"

    def apply(lhs: Exp, rhs: Exp) = new Logi2Arg(lhs, rhs, prefix)
  }

  object Not {
    def unapply(arg: Logi2Arg): Option[(Any, Any)] = {
      Some(arg.lhs, arg.rhs)
    }

    val prefix: String = "not"

    def apply(lhs: Exp) = new Logi2Arg(lhs, null, prefix)
  }

  implicit def addStreamerTup(cmp_strm: (Component, Streamer)): Streamer = Streamer(cmp_strm._2.name, cmp_strm._2.lvl, cmp_strm._2.width, cmp_strm._2.logicExp, null, cmp_strm._2.typ, -1, null, cmp_strm._1)
  implicit def addStreamerTupWithIndex(cmp_strm: (Component, Streamer, Int)): Streamer = Streamer(cmp_strm._2.name, cmp_strm._2.lvl, cmp_strm._2.width, cmp_strm._2.logicExp, null, cmp_strm._2.typ, cmp_strm._3, null, cmp_strm._1)
  implicit def addStreamerTupWithSlice(cmp_strm: (Component, Streamer, Int, Int)): Streamer = Streamer(cmp_strm._2.name, cmp_strm._2.lvl, cmp_strm._2.width, cmp_strm._2.logicExp, null, cmp_strm._2.typ, -1, (cmp_strm._3, cmp_strm._4), cmp_strm._1)

  case class Streamer(name: String, lvl: Int, override val width: Int, logicExp: Option[Any], ap: List[(Int, String)] = null, typ: PortTypeEnum = PortTypeEnum.Z, index: Int = -1, slice: (Int,Int) = null, cmp: Component = null) extends Exp  {

    // Link
    def ~>(stm: Streamer): Streamer = Streamer(s"lnk_${this.name}_${stm.name}", 1 + List(this.lvl, stm.lvl).max, width,
      Option(Link(this, stm)))
    /* def ~>(cmp_rhs: (Component, Streamer)): Streamer = Streamer(s"lnk_${this.name}_${cmp_rhs._2.name}", 1 + List(this.lvl, cmp_rhs._2.lvl).max, width,
      Option(Link(this, cmp_rhs))) */

    // Arithmetic
    def +(stm: Streamer): Streamer = Streamer(s"add_${this.name}_${stm.name}", 1 + List(this.lvl, stm.lvl).max, width,
      Option(Add(this, stm)))

    def +(const: Const): Streamer = Streamer(s"add_${this.name}_ct_${const.vl}", 1 + List(this.lvl, const.lvl).max, width,
      Option(Add(this, const)))

    /* def +(cmp: Component, stm: Streamer): Streamer = Streamer(s"add_${this.name}_${stm.name}", 1 + List(this.lvl, stm.lvl).max, width,
      Option(Add(this, cmp, stm))) */

    def -(stm: Streamer): Streamer = Streamer(s"sub_${this.name}_${stm.name}", 1 + List(this.lvl, stm.lvl).max, width,
      Option(Sub(this, stm)))

    def -(const: Const): Streamer = Streamer(s"sub_${this.name}_ct_${const.vl}", 1 + List(this.lvl, const.lvl).max, width,
      Option(Sub(this, const)))

    /* def -(cmp: Component, stm: Streamer): Streamer = Streamer(s"sub_${this.name}_${stm.name}", 1 + List(this.lvl, stm.lvl).max, width, Option(Sub(this, cmp, stm))) */

    def *(stm: Streamer): Streamer = Streamer(s"mul_${this.name}_${stm.name}", 1 + List(this.lvl, stm.lvl).max, width,
      Option(Mul(this, stm)))

    def *(const: Const): Streamer = Streamer(s"mul_${this.name}_ct_${const.vl}", 1 + List(this.lvl, const.lvl).max, width,
      Option(Mul(this, const)))

    def /(stm: Streamer): Streamer = Streamer(s"div_${this.name}_${stm.name}", 1 + List(this.lvl, stm.lvl).max, width,
      Option(Div(this, stm)))

    def /(const: Const): Streamer = Streamer(s"div_${this.name}_ct_${const.vl}", 1 + List(this.lvl, const.lvl).max, width,
      Option(Div(this, const)))

    // Logical
    def <=(stm: Streamer): Streamer = Streamer(s"le_${this.name}_${stm.name}", 1 + List(this.lvl, stm.lvl).max, width,
      Option(Le(this, stm)))

    def <=(const: Const): Streamer = Streamer(s"le_${this.name}_ct_${const.vl}", 1 + List(this.lvl, const.lvl).max, width,
      Option(Le(this, const)))

    def &&(stm: Streamer): Streamer = Streamer(s"and_${this.name}_${stm.name}", 1 + List(this.lvl, stm.lvl).max, width,
      Option(And(this, stm)))

    def &&(const: Const): Streamer = Streamer(s"and_${this.name}_ct_${const.vl}", 1 + List(this.lvl, const.lvl).max, width,
      Option(And(this, const)))

    def ||(stm: Streamer): Streamer = Streamer(s"or_${this.name}_${stm.name}", 1 + List(this.lvl, stm.lvl).max, width,
      Option(Or(this, stm)))

    def ||(const: Const): Streamer = Streamer(s"or_${this.name}_ct_${const.vl}", 1 + List(this.lvl, const.lvl).max, width,
      Option(Or(this, const)))

    def !(): Streamer = Streamer(s"not_${this.name}", 1 + this.lvl, width, Option(Not(this)))
  }

  // (label: String, lvl: Int, logicExp: Option[Any])
  object Streamer {
    // Basic Operations

    def apply(id: String, dWidth: Int): Streamer = new Streamer(id, 0, dWidth, Option(dWidth))

    def apply(id: String, dWidth: Int, typ: PortTypeEnum): Streamer = new Streamer(id, 0, dWidth, Option(dWidth), null, typ)

    def apply(): Streamer = null

    def isAllDigits(x: String) = x forall Character.isDigit
  }

  case class Const(vl: Int, lvl: Int, override val width: Int = 0) extends Exp

  object Const {
    implicit def apply(i: Int) = new Const(i, 0)
  }

  case class Arith2Arg(lhs: Exp, rhs: Exp, prfx: String) extends Exp {
    override val level: Int = List(lhs.level, rhs.level).max
  }

  case class Logi2Arg(lhs: Exp, rhs: Exp, prfx: String) extends Exp {
    override val level: Int = List(lhs.level, if(rhs != null) rhs.level else 0).max
  }

  case class Link2Arg(src: Streamer, tgt: Streamer) extends Exp {
    override val level: Int = List(src.level, tgt.level).max
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Controllers
  //--------------------------------------------------------------------------------------------------------------------

  //------ Finite State Machine (FSM)

  implicit def addState(name: String) = new State(name)

  case class Fsm(name: String) {
    var _name: String = name
    var _firstState: State = null
    var _states: Seq[State] = null

    def starting(s: State) = {
      _firstState = s
      this
    }

    def has(xs: State*) = {
      _states = xs
      this
    }
  }

  case class State(name: String) {
    var _name: String = name
    var _task: Seq[Any] = Seq()
    var _transCheck: Set[Any] = Set()
    var _to: Set[State] = Set()

    def using(cb: Seq[Any]): State = {
      _task = cb
      this
    }

    def goto(s: State*) = {
      _to = _to ++ s
      this
    }

    def when(x: Any*) = {
      _transCheck = x.toSet
      this
    }
  }

  //------ If Statement
  case class If(cs: Any)(cb: Seq[Any]) {
    val _name = randomStr(6)
    val _ifCheck = cs
    var _codeBlock = cb
    var _elsIfs: List[(String, Any, Seq[Any])] = null
    var _els: (String, Seq[Any]) = null

    def elsIf(cd: Any, xs: Seq[Any]) = {
      if (_elsIfs == null) _elsIfs = List()
      _elsIfs = _elsIfs ::: List((randomStr(6), cd, xs))
      this
    }

    def els(cb: Seq[Any]) = {
      _els = (randomStr(6), cb)
      this
    }
  }

}
