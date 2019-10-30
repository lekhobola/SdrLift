package sdrlift.codegen.vhdl

import de.upb.hni.vmagic.{AssociationElement, Range, SubtypeDiscreteRange, VhdlFile, expression}
import de.upb.hni.vmagic.`object`.{ArrayElement, AttributeExpression, Constant, Signal}
import de.upb.hni.vmagic.builtin.{Standard, StdLogic1164}
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
import de.upb.hni.vmagic.literal._
import NodeFactory._
import de.upb.hni.vmagic.`type`.IndexSubtypeIndication
import de.upb.hni.vmagic.expression.{Add => _, And => _, Or => _, _}
import de.upb.hni.vmagic.expression.{Add, And, Divide, LessThan, Multiply, Not, Or, Subtract}
import de.upb.hni.vmagic.output.VhdlOutput
import de.upb.hni.vmagic.util.VhdlCollections
import exp.CompExp
import exp.CompExp.ConstVal
import exp.NodeExp.{Arith2Arg, Logi2Arg, Streamer}
import scalax.collection.GraphTraversal.{BreadthFirst, DepthFirst, Predecessors}
import sdrlift.analysis.GrphAnalysis
import sdrlift.analysis.GrphAnalysis.removeDiNode

import scala.collection.Seq
import scala.collection.immutable.Map
//import de.upb.hni.vmagic.expression.{Add, Subtract, Divide, Or, And, Multiply, LessThan}
import de.upb.hni.vmagic.highlevel.StateMachine
import sdrlift.analysis.GrphAnalysis._
import scala.collection.JavaConversions._

case class VhdlPrimitives(dfg: Graph[DfgNode, DfgEdge]) {

  // get the Arithmetic or Logical Expression
  def getHdlLogicalBinaryExp(tgtNode: Node, lhsHdlExp: BinaryExpression[_], rhsHdlExp: BinaryExpression[_])
  : BinaryExpression[_] = tgtNode.operator match {
    // Arithmetic
    case "add" => new Add(lhsHdlExp, rhsHdlExp)
    case "sub" => new Subtract(lhsHdlExp, rhsHdlExp)
    case "mul" => new Multiply(lhsHdlExp, rhsHdlExp)
    case "div" => new Divide(lhsHdlExp, rhsHdlExp)
    // Logical
    case "and" => new And(lhsHdlExp, rhsHdlExp)
    case "or" => new Or(lhsHdlExp, rhsHdlExp)
    // Comparison
    case "le" | "sc_le" => new LessThan(lhsHdlExp, rhsHdlExp)
    case _ => null
  }

  def getHdlLogicalUnaryExp(tgtNode: Node, lhsHdlExp: BinaryExpression[_])
  : Expression[_] = tgtNode.operator match {
    case "not" => new Not(lhsHdlExp)
    case _ => null
  }

  def buildCondBinHdl(root: Node, accBinExp: BinaryExpression[_] = null): BinaryExpression[_] = root match {
    case (lgcNode: LogiNode) => {
      val srcNodeList = (dfg get lgcNode).incoming.toList.map(_.source).toList
      val lhsNode = srcNodeList.filter(_.name.equals(lgcNode.lhs)).head
      val rhsNode = srcNodeList.filter(_.name.equals(lgcNode.rhs)).head
      (root, lhsNode.value, rhsNode.value) match {
        case (tgtLgcNode: LogiNode, lhsStrm: StrmNode, rhsStrm: StrmNode) => {
          getHdlBinaryExp(tgtLgcNode, lhsStrm.name, rhsStrm.name)
        }
        case (tgtLgcNode: LogiNode, lhsStrm: StrmNode, rhsArith: ArithNode) => {
          getHdlBinaryExp(tgtLgcNode, lhsStrm.name, rhsArith.name)
        }
        case (tgtLgcNode: LogiNode, lhsStrm: StrmNode, rhsCnst: ConstNode) => {
          getHdlBinaryExp(tgtLgcNode, lhsStrm.name, rhsCnst.name)
        }
        case (tgtLgcNode: LogiNode, lhs: LogiNode, rhs: LogiNode) => {
          val lhsBinExp = buildCondBinHdl(lhs, accBinExp)
          val rhsBinExp = buildCondBinHdl(rhs, accBinExp)
          getHdlLogicalBinaryExp(tgtLgcNode, lhsBinExp, rhsBinExp)
        }
      }
    }
    case _ => accBinExp
  }

  def getHdlBinaryExp(tgtNode: Node, lhsId: String, rhsId: String): BinaryExpression[_] = {
    val hdlLhsExp = new Signal(lhsId, StdLogic1164.STD_LOGIC) //if (!isBinary(lhsId)) new Signal(lhsId, StdLogic1164.STD_LOGIC) else new BinaryLiteral(lhsId)
    val hdlRhsExp = new Signal(rhsId, StdLogic1164.STD_LOGIC) // if (!isBinary(rhsId)) new Signal(rhsId, StdLogic1164.STD_LOGIC) else new BinaryLiteral(rhsId)
    tgtNode.operator match {
      // Arithmetic
      case "add" => {
        (isBinary(lhsId), isBinary(rhsId)) match {
          case (true, true) => new Add(new BinaryLiteral(lhsId), new BinaryLiteral(rhsId))
          case (true, false) => new Add(new BinaryLiteral(lhsId), hdlRhsExp)
          case (false, true) => new Add(hdlLhsExp, new BinaryLiteral(rhsId))
          case (false, false) => new Add(hdlLhsExp, hdlRhsExp)
        }
      }
      case "sub" => {
        (isBinary(lhsId), isBinary(rhsId)) match {
          case (true, true) => new Subtract(new BinaryLiteral(lhsId), new BinaryLiteral(rhsId))
          case (true, false) => new Subtract(new BinaryLiteral(lhsId), hdlRhsExp)
          case (false, true) => new Subtract(hdlLhsExp, new BinaryLiteral(rhsId))
          case (false, false) => new Subtract(hdlLhsExp, hdlRhsExp)
        }
      }
      case "mul" => {
        (isBinary(lhsId), isBinary(rhsId)) match {
          case (true, true) => new Multiply(new BinaryLiteral(lhsId), new BinaryLiteral(rhsId))
          case (true, false) => new Multiply(new BinaryLiteral(lhsId), hdlRhsExp)
          case (false, true) => new Multiply(hdlLhsExp, new BinaryLiteral(rhsId))
          case (false, false) => new Multiply(hdlLhsExp, hdlRhsExp)
        }
      }
      case "div" => {
        (isBinary(lhsId), isBinary(rhsId)) match {
          case (true, true) => new Divide(new BinaryLiteral(lhsId), new BinaryLiteral(rhsId))
          case (true, false) => new Divide(new BinaryLiteral(lhsId), hdlRhsExp)
          case (false, true) => new Divide(hdlLhsExp, new BinaryLiteral(rhsId))
          case (false, false) => new Divide(hdlLhsExp, hdlRhsExp)
        }
      }
      // Logical
      case "and" => {
        (isBinary(lhsId), isBinary(rhsId)) match {
          case (true, true) => new And(new BinaryLiteral(lhsId), new BinaryLiteral(rhsId))
          case (true, false) => new And(new BinaryLiteral(lhsId), hdlRhsExp)
          case (false, true) => new And(hdlLhsExp, new BinaryLiteral(rhsId))
          case (false, false) => new And(hdlLhsExp, hdlRhsExp)
        }
      }
      case "or" => {
        (isBinary(lhsId), isBinary(rhsId)) match {
          case (true, true) => new Or(new BinaryLiteral(lhsId), new BinaryLiteral(rhsId))
          case (true, false) => new Or(new BinaryLiteral(lhsId), hdlRhsExp)
          case (false, true) => new Or(hdlLhsExp, new BinaryLiteral(rhsId))
          case (false, false) => new Or(hdlLhsExp, hdlRhsExp)
        }
      }
      // Comparison
      case "le" => {
        (isBinary(lhsId), isBinary(rhsId)) match {
          case (true, true) => new LessThan(new BinaryLiteral(lhsId), new BinaryLiteral(rhsId))
          case (true, false) => new LessThan(new BinaryLiteral(lhsId), hdlRhsExp)
          case (false, true) => new LessThan(hdlLhsExp, new BinaryLiteral(rhsId))
          case (false, false) => new LessThan(hdlLhsExp, hdlRhsExp)
        }
      }
      case _ => null
    }
  }

  def getHdlUnaryExp(tgtNode: Node, argNode: String): Expression[_] = tgtNode.operator
  match {
    case "not" => new Not(new Signal(argNode, StdLogic1164.STD_LOGIC))
    case _ => null
  }

  def getHdlBinConstExp(tgtNode: Node, argNode1: String, argNode2: String): BinaryExpression[_] = tgtNode.operator
  match {
    // Arithmetic
    case "add" => new Add(new Signal(argNode1, StdLogic1164.STD_LOGIC), new BinaryLiteral(argNode2))
    case "sub" => new Subtract(new Signal(argNode1, StdLogic1164.STD_LOGIC), new BinaryLiteral(argNode2))
    case "mul" => new Multiply(new Signal(argNode1, StdLogic1164.STD_LOGIC), new BinaryLiteral(argNode2))
    case "div" => new Divide(new Signal(argNode1, StdLogic1164.STD_LOGIC), new BinaryLiteral(argNode2))
    // Logical
    case "and" => new And(new Signal(argNode1, StdLogic1164.STD_LOGIC), new BinaryLiteral(argNode2))
    case "or" => new Or(new Signal(argNode1, StdLogic1164.STD_LOGIC), new BinaryLiteral(argNode2))
    // Comparison
    case "le" => new LessThan(new Signal(argNode1, StdLogic1164.STD_LOGIC), new BinaryLiteral(argNode2))
    case _ => null
  }

  // get Arithmetic and Logic Conditional Signal assignment
  def getCSA(tgtNode: Node, lhsId: String, rhdId: String) = {
    val sgnlTyp = if (tgtNode.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(tgtNode.width)
    val hdltgtNodeSgnl = new Signal(tgtNode.inst, sgnlTyp)
    val hdlOpExp = getHdlBinaryExp(tgtNode, lhsId, rhdId)
    new ConditionalSignalAssignment(hdltgtNodeSgnl, hdlOpExp)
  }

  def getNotCSA(tgtNode: Node) = {
    val sgnlTyp = if (tgtNode.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(tgtNode.width)
    val hdltgtNodeSgnl = new Signal(tgtNode.inst, sgnlTyp)
    val argLabel = getPredNodeLabel(tgtNode, ((dfg get tgtNode) incoming).head.source)
    val hdlOpSgnl = new Signal(argLabel, StdLogic1164.STD_LOGIC)

    new ConditionalSignalAssignment(hdltgtNodeSgnl, new Not(hdlOpSgnl))
  }


  def getConstCSA(tgtNode: Node, argNode1: String, argNode2: String) = {
    val hdltgtNodeSgnl1 = new Signal(tgtNode.inst, StdLogic1164.STD_LOGIC)
    val hdlOpExp = getHdlBinConstExp(tgtNode, argNode1, argNode2)

    new ConditionalSignalAssignment(hdltgtNodeSgnl1, hdlOpExp)
  }

  // get Sequential Arithmetic and Logic Signal assignment
  def getAlSA(tgtNode: Node, argNode1: String, argNode2: String) = {
    val hdltgtNodeSgnl1 = new Signal(tgtNode.inst, StdLogic1164.STD_LOGIC)
    val hdlOpExp = getHdlBinaryExp(tgtNode, argNode1, argNode2)

    new SignalAssignment(hdltgtNodeSgnl1, hdlOpExp)
  }

  def getPredNodeLabel(binNode: Node, predNode: DfgNode): String = predNode.value match {
    case stmNode: StrmNode => stmNode.name
    case arithNode: ArithNode => arithNode.name
    case lgcNode: LogiNode => lgcNode.name
    case cmpNode: CompNode => {
      val srcPort = ((dfg get binNode) incoming).filter(_.source.equals(cmpNode)).head.srcPort
      cmpNode.inst + "_" + srcPort.name
    }
    case tplNode: TmplNode => {
      val predLabelNode = tplNode.comp._outlinks.find(_._3.equals(binNode))
      if (predLabelNode != None)
        tplNode.inst + "_" + predLabelNode.get._2.name
      else {
        val srcPort = ((dfg get binNode) incoming).filter(_.source.equals(tplNode)).head.srcPort
        tplNode.inst + "_" + srcPort.name
      }
    }
  }

  // get VHDL Expression Lines, return (target Node, Arg1, Arg2, Level)
  def getExpLines(nodes: List[Node]): List[(Node, String, String, Int)] = {
    var tpList = List[(Node, String, String, Int)]()
    var visitedNodesList = List[String]()
    for (n <- nodes) {
      val inEdges = (dfg get n).incoming
      val srcNodeList = if (!inEdges.isEmpty) inEdges.toList.map(_.source).toList else List()
      if (!srcNodeList.isEmpty) {
        if (n.isInstanceOf[ArithNode]) {
          if (srcNodeList.size > 1 && !visitedNodesList.contains(n.inst)) {
            val arithNode = n.asInstanceOf[ArithNode]
            //val argLabel1 = getPredNodeLabel(arithNode, srcNodeList.filter(_.inst.equals(arithNode.lhs)).head)

            /* srcNodeList.map(el => println(el.inst))
            println
            println(arithNode.lhs.getLastNodeId)
            println(arithNode.rhs.getLastNodeId)
            println() */
            //println(arithNode.inst)
            val argLabel1 = getPredNodeLabel(arithNode, srcNodeList.filter(_.inst.equals(arithNode.lhs.getLastNodeId)).head)
            //val argLabel2 = getPredNodeLabel(arithNode, srcNodeList.filter(_.inst.equals(arithNode.rhs)).head)
            val argLabel2 = getPredNodeLabel(arithNode, srcNodeList.filter(_.inst.equals(arithNode.rhs.getLastNodeId)).head)
            tpList = tpList ::: List((arithNode, argLabel1, argLabel2, arithNode.level))
            visitedNodesList = visitedNodesList ::: List(n.inst)
          }
        } else if (n.isInstanceOf[LogiNode]) {
          val logicalNode = n.asInstanceOf[LogiNode]
          if (srcNodeList.size > 1 && !visitedNodesList.contains(n.inst)) {
            //val argLabel1 = getPredNodeLabel(logicalNode, srcNodeList.filter(_.inst.equals(logicalNode.lhs)).head)
            val argLabel1 = getPredNodeLabel(logicalNode, srcNodeList.filter(_.inst.equals(logicalNode.lhs.getLastNodeId)).head)
            //val argLabel2 = getPredNodeLabel(logicalNode, srcNodeList.filter(_.inst.equals(logicalNode.rhs)).head)
            val argLabel2 = getPredNodeLabel(logicalNode, srcNodeList.filter(_.inst.equals(logicalNode.rhs.getLastNodeId)).head)
            tpList = tpList ::: List((logicalNode, argLabel1, argLabel2, logicalNode.level))
            visitedNodesList = visitedNodesList ::: List(n.inst)
          } /*else if (srcNodeList.size == 1 && !visitedNodesList.contains(n.name)) {
            val argLabel = getPredNodeLabel(logicalNode, srcNodeList.filter(_.inst.equals(logicalNode.lhs)).head)
            tpList = tpList ::: List((logicalNode, argLabel, argLabel, logicalNode.level))
            visitedNodesList = visitedNodesList ::: List(n.name)
          }*/
        }
      }
    }

    tpList.sortBy(_._4)
  }

  // get Conditional Signal Assignments
  def getCSAs(cbNodes: List[Node]): List[ConditionalSignalAssignment] = {
    getExpLines(cbNodes).map { e =>
      getCSA(e._1, e._2, e._3)
    }
  }

  // build a hirarchical if-statatement
  def buildIfStmt(ifn: Node): IfStatement = {

    val ifcondNode = dfg.getDiPredNodes(ifn, List("cnd")).head
    val ifLgcNode = dfg.getDiPredNodes(ifcondNode, List("lgc")).head
    val hdlIfsCondOpExp = buildCondBinHdl(ifLgcNode)
    val hdlIf = new IfStatement(hdlIfsCondOpExp)

    // code block
    val ifsCodeBlocks = dfg.getCodeBlocksFiltNot(ifn, List("if", "elsif", "els", "sx", "lgc", "cnd"))
    getExpLines(ifsCodeBlocks).foreach { e =>
      hdlIf.getStatements.add(getAlSA(e._1, e._2, e._3))
    }
    // sub-statements
    val ifsSubBlocks = dfg.getDiPredNodes(ifn, List("if"))
    ifsSubBlocks.foreach { e =>
      hdlIf.getStatements.add(buildIfStmt(e))
    }

    // add else-if-statements and els-statement
    var eifCount = 0

    //  val ifPredNodes = if (leafNodes.find(n => n.name.equals(ifn.name)) != None)
    val ifPredNodes = dfg.getPredecessorNodes(ifn, List("elsif", "els"))
    ifPredNodes.foreach { ef =>
      (ef) match {
        case (elf: ElsIfNode) => {
          val eifNode = elf.asInstanceOf[ElsIfNode]
          val eifcondNode = dfg.getDiPredNodes(eifNode, List("cnd")).head
          val eifLgcNode = dfg.getDiPredNodes(eifcondNode, List("lgc")).head
          val hdlElsCondOpExp = buildCondBinHdl(eifLgcNode)
          hdlIf.createElsifPart(hdlElsCondOpExp)
          // code block
          val elsifCodeBlocks = dfg.getCodeBlocksFiltNot(elf, List("if", "elsif", "els", "sx", "lgc", "cnd"))
          getExpLines(elsifCodeBlocks).foreach { e =>
            hdlIf.getElsifParts.get(eifCount).getStatements.add(getAlSA(e._1, e._2, e._3))
          }
          // sub-statements
          val elsifSubBlocks = dfg.getDiPredNodes(elf, List("if"))
          elsifSubBlocks.foreach { e =>
            hdlIf.getElsifParts.get(eifCount).getStatements.add(buildIfStmt(e))
          }
          eifCount = eifCount + 1
        }
        case (el: ElsNode) => {
          // code block
          val elCodeBlocks = dfg.getCodeBlocksFiltNot(el, List("if", "elsif", "els", "sx", "lgc", "cnd"))
          getExpLines(elCodeBlocks).foreach { e =>
            hdlIf.getElseStatements.add(getAlSA(e._1, e._2, e._3))
          }
          // sub-statements
          val elSubBlocks = dfg.getDiPredNodes(el, List("if"))
          elSubBlocks.foreach { e =>
            hdlIf.getElseStatements.add(buildIfStmt(e))
          }
        }
        case _ => null
      }
    }

    hdlIf
  }

  private def isAllDigits(x: String) = x forall Character.isDigit

  // generic type
  def hdlLiteralVal(aParam: Any) = {
    val param = aParam
    aParam match {
      case d: Integer => new DecimalLiteral(param.asInstanceOf[Int])
      case c: Character => new CharacterLiteral(param.asInstanceOf[Character])
      case s: String => {
        val strParam = param.asInstanceOf[String]
        val physicalArr = ("""\(.*?\)""".r findAllIn strParam).toList
        if (!physicalArr.isEmpty) {
          new PhysicalLiteral(physicalArr.head)
        }
        else if (strParam.substring(0, 1).equalsIgnoreCase("b"))
          new BinaryLiteral(strParam.substring(1, strParam.length))
        else if (strParam.substring(0, 1).equalsIgnoreCase("c"))
          new CharacterLiteral(strParam.charAt(1))
        else if (strParam.substring(0, 1).equalsIgnoreCase("x"))
          new HexLiteral(strParam.substring(1, strParam.length))
        else if (isAllDigits(strParam)) new DecimalLiteral(strParam.toInt)
        else new StringLiteral(strParam)
      }
      // case "class java.lang.Boolean"    =>  new EnumerationLiteral(param.asInstanceOf[Boolean])
      case _ => new BinaryLiteral(param.asInstanceOf[String])
    }
  }

  def getSgnlCardinal(sgnl: Signal) = sgnl.getType.asInstanceOf[IndexSubtypeIndication].
    getRanges.get(0).asInstanceOf[Range].getFrom

  def getSgnlMsbVal(sgnl: Signal): Int = {
    if (sgnl.getType.isInstanceOf[IndexSubtypeIndication]) {
      val sgnlCardinal = getSgnlCardinal(sgnl)
      if (sgnlCardinal.isInstanceOf[Subtract]) {
        val msb = sgnlCardinal.asInstanceOf[Subtract].getLeft
        if (msb.isInstanceOf[Constant]) {
          if (isAllDigits(msb.asInstanceOf[Constant].getIdentifier)) {
            val arithExp = msb.asInstanceOf[Constant].getIdentifier.split("\\s").toList
            arithExpEval(arithExp)
          } else {
            -1
          }
        } else {
          msb.asInstanceOf[DecimalLiteral].getValue.toInt
        }
      } else {
        sgnlCardinal.asInstanceOf[DecimalLiteral].getValue.toInt + 1
      }
    } else {
      1
    }
  }

  def arithExpEval(expression: List[String]): Int = expression match {
    case l :: "+" :: r :: rest => arithExpEval((l.toInt + r.toInt).toString :: rest)
    case l :: "-" :: r :: rest => arithExpEval((l.toInt - r.toInt).toString :: rest)
    case value :: Nil => value.toInt
  }

  def getSgnlType(hdlGenAssEls: List[AssociationElement], portSgnl: Signal, width: Int) = {
    if (portSgnl.getType.isInstanceOf[IndexSubtypeIndication]) {
      val sgnlCardinal = getSgnlCardinal(portSgnl)
      if (sgnlCardinal.isInstanceOf[Subtract]) {
        val hdlGenId = sgnlCardinal.asInstanceOf[Subtract].getLeft.asInstanceOf[Constant].getIdentifier
        val hdlGenAssEl = hdlGenAssEls.find(_.getFormal.equals(hdlGenId))
        if (hdlGenAssEl != None) {
          val msb = sgnlCardinal.asInstanceOf[Subtract].getLeft
          if (isAllDigits(msb.asInstanceOf[Constant].getIdentifier)) {
            val arithExp = msb.asInstanceOf[Constant].getIdentifier.split("\\s").toList
            val res = arithExpEval(arithExp)
            StdLogic1164.STD_LOGIC_VECTOR(res)
          } else {
            StdLogic1164.STD_LOGIC_VECTOR(hdlGenAssEl.get.getActual)
          }
        }
        else {
          portSgnl.getType
        }
      } else {
        portSgnl.getType
      }
    } else {
      if (width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(width)
    }
  }

  def getCompSgnlGenericVal(hdlGenAssEls: List[AssociationElement], sgnl: Signal) = {
    if (sgnl.getType.isInstanceOf[IndexSubtypeIndication]) {
      val sgnlCardinal = getSgnlCardinal(sgnl)
      if (sgnlCardinal.isInstanceOf[Subtract]) {
        val hdlGenId = sgnlCardinal.asInstanceOf[Subtract].getLeft.asInstanceOf[Constant].getIdentifier
        val hdlGenAssEl = hdlGenAssEls.find(_.getFormal.equals(hdlGenId))
        if (hdlGenAssEl != None) {
          val msb = sgnlCardinal.asInstanceOf[Subtract].getLeft
          if (isAllDigits(msb.asInstanceOf[Constant].getIdentifier)) {
            val arithExp = msb.asInstanceOf[Constant].getIdentifier.split("\\s").toList
            arithExpEval(arithExp)
          } else {
            hdlGenAssEl.get.getActual.asInstanceOf[DecimalLiteral].getValue.toInt
          }
        } else {
          sgnlCardinal.asInstanceOf[DecimalLiteral].getValue.toInt + 1
        }
      } else {
        sgnlCardinal.asInstanceOf[DecimalLiteral].getValue.toInt + 1
      }
    } else {
      1
    }
  }

  def evalSgnlType(sgnl: Signal) = {
    if (sgnl.getType.isInstanceOf[IndexSubtypeIndication]) {
      val sgnlCardinal = getSgnlCardinal(sgnl)
      if (sgnlCardinal.isInstanceOf[Subtract]) {
        val msb = sgnlCardinal.asInstanceOf[Subtract].getLeft
        if (msb.isInstanceOf[DecimalLiteral]) {
          val arithExp = msb.asInstanceOf[DecimalLiteral].getValue.split("\\s").toList
          val res = arithExpEval(arithExp)
          StdLogic1164.STD_LOGIC_VECTOR(res)
        } else {
          sgnl.getType
        }
      } else {
        sgnl.getType
      }
    } else {
      sgnl.getType
    }
  }

  def getRoundedCSA(left: Signal, right: Signal) = {
    def sgnlCardinal(sgnl: Signal) = {
      if (sgnl.getType.isInstanceOf[IndexSubtypeIndication]) {
        val sgnlCardinal = getSgnlCardinal(sgnl)
        if (sgnlCardinal.isInstanceOf[Subtract]) {
          val msb = sgnlCardinal.asInstanceOf[Subtract].getLeft
          if (isAllDigits(msb.asInstanceOf[Constant].getIdentifier))
            new DecimalLiteral(msb.asInstanceOf[Constant].getIdentifier.toInt)
          else
            sgnlCardinal.asInstanceOf[Subtract].getLeft
        } else {
          new DecimalLiteral(sgnlCardinal.asInstanceOf[DecimalLiteral].getValue.toInt + 1)
        }
      } else {
        new DecimalLiteral(1)
      }
    }

    val leftCardinal = sgnlCardinal(left)
    val rightCardinal = sgnlCardinal(right)

    if (leftCardinal.isInstanceOf[DecimalLiteral] && rightCardinal.isInstanceOf[DecimalLiteral]) {
      val rightWidth = rightCardinal.asInstanceOf[DecimalLiteral].getValue.toInt
      val leftWidth = leftCardinal.asInstanceOf[DecimalLiteral].getValue.toInt
      if (leftWidth < rightWidth) {
        new ConditionalSignalAssignment(left, right.getSlice(new Range(new DecimalLiteral(rightWidth - 1), Range.Direction.DOWNTO, new DecimalLiteral(rightWidth - leftWidth))))
      } else {
        new ConditionalSignalAssignment(left, right)
      }
    } else {
      new ConditionalSignalAssignment(left, right)
    }
  }

  def toBinary(i: Int, digits: Int = 8) = {
    val binStr = String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')
    binStr.substring(binStr.length - digits, binStr.length)
  }

  def isBinary(x: String) = x forall (c => (c == '0' || c == '1'))

  // (String, Boolean) = (SgnlLabel, BinaryLiteral,SgnlId)
  def getNodeDoutExpLabel(n: DfgNode, appendableNodeInst: Boolean = false): (String, Boolean) = n match {
    case arNode: ArithNode => (n.inst, false)
    case cNode: CompNode => {
      null
    }
    case tNode: TmplNode => {
      tNode.name match {
        case "ConstVal" => (toBinary(tNode.comp._params.get("CONSTVALUE").get.asInstanceOf[Int], tNode.width), true)
        case "DummyConst" => (tNode.inst, false)
        case _ => {
          val nodeInst = if (appendableNodeInst) tNode.inst + "_" else ""
          (nodeInst + tNode.comp._outports.find(p => p.typ == PortTypeEnum.DOUT).get.name, false)
        }
      }
    }
    case _ => null
  }

  def findLast[A](la: Seq[A])(f: A => Boolean): Option[A] =
    la.foldLeft(Option.empty[A]) { (acc, cur) =>
      if (f(cur)) Some(cur)
      else acc
    }

  def getPortType(width: Int) = {
    if (width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(width)
  }

  def createCompsInstancesV1(arch: Architecture, entity: Entity, nodes: Seq[Node], roots: Seq[Node], leaves: Seq[Node], branches: Seq[Node]): Unit = {
    val topInNodes = roots.map { r =>
      r match {
        case compNode: CompNode => compNode.comp.dfg.getRoots.map { n => (r, StrmNode(Streamer(n.name, n.level, n.width, null, null, n.typ))) }
        case tmplNode: TmplNode => {
          if (tmplNode.comp._inports != null) tmplNode.comp._inports.map(n => (r, StrmNode(Streamer(n.name, n.lvl, n.width, null, null, n.typ)))) else List.empty
        }
        case _ => List.empty
      }
    }.flatten.distinct

    val topOutNodes = dfg.getDiPredNodes(leaves.head).map { l =>
      l match {
        case compNode: CompNode => compNode.comp.dfg.getArithLeafNodes.map { n => (l, StrmNode(Streamer(n.name, n.level, n.width, null, null, n.typ))) }
        case tmplNode: TmplNode => tmplNode.comp._outports.map(n => (l, StrmNode(Streamer(n.name, n.lvl, n.width, null, null, n.typ))))
        case arithNode: ArithNode => List((arithNode, arithNode))
        //case stm: Streamer => List((stm, stm))
        case _ => List.empty
      }
    }.flatten.distinct

    val outdir = "out/vhdl/"

    /** declare the node components & port register, and instantiate node components */
    val fixedSgnls = List("clk", "rst", "en", "vld")
    val ctrlSgnls = List("en", "vld")
    val sysSgnls = List("clk", "rst")
    val ifcSignals = topInNodes.map { k => k._1.inst + "_" + k._2.name }.toList :::
      topOutNodes.map { k => k._1.inst + "_" + k._2.name }.toList

    var declaredActors: List[String] = List()
    var declaredHdlMappedSgnlIds: List[String] = List()
    //branches.map{_.inst}.toList
    var entitySysSgnls: List[Signal] = List()

    nodes.foreach { n =>
      n match {
        /* case arithNode: ArithNode => {
          val inEdges =
            dfg.getDiPredEdges(n).map(e => (e._1, e._2.asInstanceOf[DfgNode], e._3, e._4.asInstanceOf[DfgNode])).toList

          val arg1 = inEdges.head._1
          val arg1Label = getNodeDoutExpLabel(arg1, true)
          val arg2 = inEdges.last._1
          val arg2Label = getNodeDoutExpLabel(arg2, true)

          if (arg1Label._2)
            arch.getStatements.add(getConstCSA(arithNode, arg2Label._1, arg1Label._1))
          else if (arg2Label._2)
            arch.getStatements.add(getConstCSA(arithNode, arg1Label._1, arg2Label._1))
          else
            arch.getStatements.add(getCSA(arithNode, arg1Label._1, arg2Label._1))

        } */
        case _ => {
          // declare the node components
          val hdlfile = n match {
            case compNode: CompNode => compNode.vhdlfile
            case tmplNode: TmplNode => if (!tmplNode.comp.isInstanceOf[ConstVal]) tmplNode.vhdlfile else null
            case _ => println("No HdlGen"); null
          }

          if (hdlfile != null) {
            //val hdlfile = hdlgen.getHdlFile()
            val hdlEntity = VhdlCollections.getAll(hdlfile.getElements, classOf[Entity]).head
            val hdlComp = new Component(hdlEntity)

            if (!declaredActors.contains(hdlComp.getIdentifier)) {
              arch.getDeclarations.add(hdlComp)
              declaredActors = declaredActors ::: List(hdlComp.getIdentifier)

              // Write Component VHDL file output to the directory
              VhdlOutput.toFile(hdlfile, outdir + hdlComp.getIdentifier + ".vhd")
            }

            val instance: ComponentInstantiation = new ComponentInstantiation(n.inst, hdlComp)
            var hdlGenAssEls: List[AssociationElement] = List()

            //Map Generics for the rest of the Nodes
            val portGenerics = hdlEntity.getGeneric.toList.distinct.map(_.getVhdlObjects.get(0))
            for (portGeneric <- portGenerics) {
              val params: Map[String, Any] = n match {
                case tplNode: TmplNode => tplNode.comp._params.map { case (k, v) => k.toUpperCase -> v }
                case _ => Map[String, Int]()
              }

              if (!params.isEmpty && (params.get("CONSTVALUE") == None)) {
                val genericVal = params.get(portGeneric.getIdentifier).get
                val hdlGenAssEl = new AssociationElement(portGeneric.getIdentifier, hdlLiteralVal(genericVal))
                hdlGenAssEls = hdlGenAssEls ::: List(hdlGenAssEl)
                instance.getGenericMap.add(hdlGenAssEl)
              }
            }

            // Map Signals
            val portSgnls = hdlEntity.getPort.toList.distinct.map(_.getVhdlObjects.get(0))
            entitySysSgnls = entitySysSgnls ::: portSgnls.filter(p => sysSgnls.contains(p.getIdentifier))
            for (portSgnl <- portSgnls) {
              var hdlMappedSgnl: (Signal, Int) = (portSgnl, -1)
              if (!sysSgnls.contains(portSgnl.getIdentifier)) {
                if (!ifcSignals.contains(n.inst + "_" + portSgnl.getIdentifier)) {
                  val inEdges = dfg.getDiPredEdges(n).map(e => (e._1, e._2, e._3, e._4)).toList
                  val findEdge = inEdges.find(_._4.name.equalsIgnoreCase(portSgnl.getIdentifier))
                  if (findEdge == None || ctrlSgnls.contains(portSgnl.getIdentifier)) {
                    if (portSgnl.getIdentifier.equals("en")) { // TODO: Hacked
                      val diPredNodes = dfg.getDiPredNodes(n)
                      val diPredNode = diPredNodes.head
                      //hdlMappedSgnl = new Signal(diPredNode.inst + "_vld", portSgnl.getType)
                    } else {
                      hdlMappedSgnl = (new Signal(n.inst + "_" + portSgnl.getIdentifier, portSgnl.getType), -1)
                      // check if there is links to entity interface output ports
                      n match {
                        case compNode: CompNode => {
                          // inputs
                          if (!compNode.comp._inlinks.isEmpty) {
                            val lnk = compNode.comp._inlinks.find(_._2.name.toLowerCase.equals(portSgnl.getIdentifier.toLowerCase))
                            if (lnk != None) {
                              lnk.get._3 match {
                                case stmNode: StrmNode => {
                                  val sgnlTyp = if (stmNode.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(lnk.get._4.width)
                                  val hdlDinSgnl = new Signal(stmNode.inst, Mode.IN, sgnlTyp) //portSgnl.getType)
                                  hdlMappedSgnl = (new Signal(n.inst + "_" + portSgnl.getIdentifier, sgnlTyp), -1)
                                  arch.getStatements.add(new ConditionalSignalAssignment(hdlDinSgnl, hdlMappedSgnl._1))
                                  entity.getPort.add(hdlDinSgnl)
                                }
                                case compNode: CompNode => {
                                  val sgnlTyp = getSgnlType(hdlGenAssEls, portSgnl, lnk.get._4.width)
                                  hdlMappedSgnl = (new Signal(n.inst + "_" + portSgnl.getIdentifier, sgnlTyp), -1)
                                }
                                case tmplNode: TmplNode => {
                                  val sgnlTyp = getSgnlType(hdlGenAssEls, portSgnl, lnk.get._4.width)
                                  hdlMappedSgnl = (new Signal(n.inst + "_" + portSgnl.getIdentifier, sgnlTyp), -1)
                                }
                              }
                            }

                          }
                          //outputs
                          if (!compNode.comp._outlinks.isEmpty) {
                            val lnk = compNode.comp._outlinks.find(_._2.name.toLowerCase.equals(portSgnl.getIdentifier.toLowerCase))
                            if (lnk != None) {
                              lnk.get._3 match {
                                case stmNode: StrmNode => {
                                  val sgnlTyp = if (stmNode.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(stmNode.width)
                                  val hdlDoutSgnl = new Signal(stmNode.inst, Mode.OUT, sgnlTyp) //portSgnl.getType)
                                  hdlMappedSgnl = (new Signal(n.inst + "_" + portSgnl.getIdentifier, sgnlTyp), -1)
                                  arch.getStatements.add(new ConditionalSignalAssignment(hdlDoutSgnl, hdlMappedSgnl._1))
                                  entity.getPort.add(hdlDoutSgnl)
                                }
                                case compNode: CompNode => {
                                  val sgnlTyp = getSgnlType(hdlGenAssEls, portSgnl, lnk.get._4.width)
                                  hdlMappedSgnl = (new Signal(n.inst + "_" + portSgnl.getIdentifier, sgnlTyp), -1)
                                }
                                case tmplNode: TmplNode => {
                                  val sgnlTyp = getSgnlType(hdlGenAssEls, portSgnl, lnk.get._4.width)
                                  hdlMappedSgnl = (new Signal(n.inst + "_" + portSgnl.getIdentifier, sgnlTyp), -1)
                                }
                              }
                            }
                          }
                        }
                        case tmplNode: TmplNode => {
                          // inputs
                          if (!tmplNode.comp._inlinks.isEmpty) {
                            val lnk = tmplNode.comp._inlinks.find(_._2.name.toLowerCase.equals(portSgnl.getIdentifier.toLowerCase))
                            if (lnk != None) {
                              lnk.get._3 match {
                                case stmNode: StrmNode => {
                                  val sgnlTyp = if (stmNode.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(stmNode.width)
                                  val hdlDinSgnl = new Signal(stmNode.inst, Mode.IN, sgnlTyp) //portSgnl.getType)
                                  arch.getDeclarations.add(new SignalDeclaration(hdlDinSgnl))
                                  hdlMappedSgnl = (new Signal(n.inst + "_" + portSgnl.getIdentifier, sgnlTyp), -1)
                                  arch.getStatements.add(new ConditionalSignalAssignment(hdlDinSgnl, hdlMappedSgnl._1))
                                  entity.getPort.add(hdlDinSgnl)
                                }
                                case compNode: CompNode => {
                                  val sgnlTyp = getSgnlType(hdlGenAssEls, portSgnl, lnk.get._4.width)
                                  hdlMappedSgnl = (new Signal(n.inst + "_" + portSgnl.getIdentifier, sgnlTyp), -1)
                                }
                                case tmplNode: TmplNode => {
                                  val sgnlTyp = getSgnlType(hdlGenAssEls, portSgnl, lnk.get._4.width)
                                  hdlMappedSgnl = (new Signal(n.inst + "_" + portSgnl.getIdentifier, sgnlTyp), -1)
                                }
                              }
                            }
                          }
                          //outputs
                          if (!tmplNode.comp._outlinks.isEmpty) {
                            val lnk = tmplNode.comp._outlinks.find(_._2.name.toLowerCase.equals(portSgnl.getIdentifier.toLowerCase))
                            if (lnk != None) {
                              lnk.get._3 match {
                                case stmNode: StrmNode => {
                                  val sgnlTyp = if (stmNode.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(stmNode.width)

                                  val hdlDoutSgnl = new Signal(stmNode.inst, Mode.OUT, sgnlTyp) //portSgnl.getType)
                                  arch.getDeclarations.add(new SignalDeclaration(hdlDoutSgnl))
                                  hdlMappedSgnl = (new Signal(n.inst + "_" + portSgnl.getIdentifier, sgnlTyp), -1)
                                  arch.getStatements.add(new ConditionalSignalAssignment(hdlDoutSgnl, hdlMappedSgnl._1))
                                  entity.getPort.add(hdlDoutSgnl)
                                }
                                case compNode: CompNode => {
                                  val sgnlTyp = getSgnlType(hdlGenAssEls, portSgnl, lnk.get._4.width)
                                  hdlMappedSgnl = (new Signal(n.inst + "_" + portSgnl.getIdentifier, sgnlTyp), -1)
                                }
                                case tmplNode: TmplNode => {
                                  val sgnlTyp = getSgnlType(hdlGenAssEls, portSgnl, lnk.get._4.width)
                                  hdlMappedSgnl = (new Signal(n.inst + "_" + portSgnl.getIdentifier, sgnlTyp), -1)
                                }
                              }
                            }
                          }
                        }
                        case _ => println("No Component Node"); null
                      }
                    }
                  } else {
                    if (!roots.toList.map(n => n.inst).contains(findEdge.get._2.name)) {
                      if (findEdge.get._1.isInstanceOf[ArithNode]) {
                        val sgnlTyp = if (findEdge.get._1.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(findEdge.get._1.width)
                        hdlMappedSgnl = (new Signal(findEdge.get._1.inst, sgnlTyp), -1) //portSgnl.getType)
                        declaredHdlMappedSgnlIds = declaredHdlMappedSgnlIds ::: List(hdlMappedSgnl._1.getIdentifier)
                      }
                      else if (findEdge.get._1.isInstanceOf[LogiNode]) {
                        val sgnlTyp = if (findEdge.get._1.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(findEdge.get._1.width)
                        hdlMappedSgnl = (new Signal(findEdge.get._1.inst, sgnlTyp), -1) //portSgnl.getType)
                      }
                      else {
                        //val sgnlTyp = getSgnlType(hdlGenAssEls, portSgnl, findEdge.get._2.width)
                        val sgnlTyp = if (findEdge.get._1.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(findEdge.get._2.width)
                        hdlMappedSgnl = (new Signal(findEdge.get._1.inst + "_" + findEdge.get._2.name, sgnlTyp), findEdge.get._2.index)
                      }
                    } else {
                      // signal declared in the entity as port
                      //val sgnlTyp = getSgnlType(hdlGenAssEls, portSgnl, findEdge.get._2.width)
                      val sgnlTyp = if (findEdge.get._1.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(findEdge.get._2.width)
                      hdlMappedSgnl = (new Signal(findEdge.get._2.name, sgnlTyp), -1) //portSgnl.getType)
                    }
                  }
                  // declare the node port registers
                  if (!declaredHdlMappedSgnlIds.contains(hdlMappedSgnl._1.getIdentifier)) {
                    val sgnl = hdlMappedSgnl._1
                    arch.getDeclarations.add(new SignalDeclaration(new Signal(sgnl.getIdentifier, evalSgnlType(sgnl))))
                    declaredHdlMappedSgnlIds = declaredHdlMappedSgnlIds ::: List(hdlMappedSgnl._1.getIdentifier)
                  }

                } else {
                  //Dout Signal
                  val doutSgnlMap = topOutNodes.find(_._1.inst.equals(n.inst))
                  hdlMappedSgnl = (new Signal(doutSgnlMap.get._1.inst + "_" + doutSgnlMap.get._2.name, Mode.OUT,
                    StdLogic1164.STD_LOGIC_VECTOR(doutSgnlMap.get._2.width)), -1)
                  entity.getPort.add(hdlMappedSgnl._1)
                }
              }
              // instantiate the node
              val hdlAssElIds = VhdlCollections.getAll(instance.getPortMap, classOf[AssociationElement]).map { el => (el.getFormal, el.getActual.asInstanceOf[Signal].getIdentifier) }
              val hdlAssEl = new AssociationElement(portSgnl.getIdentifier, hdlMappedSgnl._1)
              if (!hdlAssElIds.contains((hdlAssEl.getFormal, hdlAssEl.getActual.asInstanceOf[Signal].getIdentifier))) {
                if (hdlMappedSgnl._2 == -1) {
                  val formalWidth = getCompSgnlGenericVal(hdlGenAssEls, portSgnl)
                  val actualWidth = getSgnlMsbVal(hdlMappedSgnl._1)
                  if (actualWidth > -1) {
                    if (formalWidth > actualWidth) {
                      if (portSgnl.getMode == Mode.IN) {
                        val hdlExtendedMappedSgnl = new Signal(hdlMappedSgnl._1.getIdentifier + "_" + (formalWidth - 1) + "_downto_" + (formalWidth - actualWidth), StdLogic1164.STD_LOGIC_VECTOR(formalWidth))
                        arch.getDeclarations.add(new SignalDeclaration(hdlExtendedMappedSgnl))
                        val hlBl = new BasedLiteral("(" + (formalWidth - 1) + " DOWNTO " + actualWidth + " => " + hdlMappedSgnl._1.getIdentifier + "(" + (actualWidth - 1) + "))")
                        arch.getStatements.add(new ConditionalSignalAssignment(hdlExtendedMappedSgnl, new Concatenate(hlBl, hdlMappedSgnl._1)))
                        instance.getPortMap.add(new AssociationElement(portSgnl.getIdentifier, hdlExtendedMappedSgnl))
                      }
                    } else {
                      instance.getPortMap.add(hdlAssEl)
                    }
                  }
                } else { // Mapped element is std_logic and source is array element
                  val hdlIndexedMappedSgnl = new Signal(hdlMappedSgnl._1.getIdentifier + "_" + hdlMappedSgnl._2, StdLogic1164.STD_LOGIC)
                  arch.getDeclarations.add(new SignalDeclaration(hdlIndexedMappedSgnl))
                  arch.getStatements.add(new ConditionalSignalAssignment(hdlIndexedMappedSgnl, hdlMappedSgnl._1.getArrayElement(hdlMappedSgnl._2)))
                  instance.getPortMap.add(new AssociationElement(portSgnl.getIdentifier, hdlIndexedMappedSgnl))
                }
              }
            }
            arch.getStatements.add(instance)
          }
        }
      }
    }

    // add the clk and rst signals if exist in any of the components
    entitySysSgnls = entitySysSgnls.groupBy(_.getIdentifier).toList.map(_._2.head)
    entitySysSgnls.foreach { s =>
      entity.getPort.add(s)
    }

    // if one of the leaves is arithNode, assign the vld of its direct/non-direct predecessor vld signal to entity vld
    val arithLeafNodes = topOutNodes.filter(_._1.isInstanceOf[ArithNode]).toList
    // TODO: Not Complete; consider for multiple data output ports
    if (!arithLeafNodes.isEmpty) {
      val degreeNodes = dfg.degreeNodeSeq(dfg.InDegree).sortWith((x, y) => x._1 < y._1).map(p => (p._1, p._2.value))
      val diNode = findLast(degreeNodes)(p => p._2.isInstanceOf[CompNode] || p._2.isInstanceOf[TmplNode])
      if (diNode != None) {
        val vldSgnl = new Signal("vld", Mode.OUT, StdLogic1164.STD_LOGIC)
        entity.getPort.add(vldSgnl)

        val vldSgnlCSA = new ConditionalSignalAssignment(vldSgnl, new Signal(diNode.get._2.inst + "_vld",
          StdLogic1164.STD_LOGIC))
        arch.getStatements.add(vldSgnlCSA)

        val diNodeDoutSgnl = new Signal(getNodeDoutExpLabel(diNode.get._2)._1, Mode.OUT, getPortType(arithLeafNodes.head._1.width))

        val arithLeafNodeSgnl = new Signal(arithLeafNodes.head._1.inst, getPortType(arithLeafNodes.head._1.width))
        entity.getPort.add(diNodeDoutSgnl)
        arch.getStatements.add(new ConditionalSignalAssignment(diNodeDoutSgnl, arithLeafNodeSgnl))
      }
    }
  }

  def combHdlGen(arch: Architecture, entity: Entity, g: Graph[DfgNode, DfgEdge]): Unit = {
    val grph = removeDiNode(g, entity.getIdentifier)
    val roots = grph.getRoots
    val leaves = grph.getLeaves

   // grph.schematic(entity.getIdentifier)
    grph.drawIR(entity.getIdentifier)

    val branches = grph.getBranches
    val vhdlPrimitives = VhdlPrimitives(grph)
    val nodes = grph.getOrderedNodes.distinct
    val outdir = "out/vhdl/"

    def getAssocElTup(hdlPortSrcSgnl: Signal, tgtSgnlWidth: Int, hdlPortTgtSgnl: Signal, srcSgnlWidth: Int)= {
      if (tgtSgnlWidth > srcSgnlWidth) {
        val hdlExtPortSrcSgnl = new Signal(hdlPortSrcSgnl.getIdentifier + "_" + (tgtSgnlWidth - 1) +
          "_downto_" + (tgtSgnlWidth - srcSgnlWidth), StdLogic1164.STD_LOGIC_VECTOR(tgtSgnlWidth))
        arch.getDeclarations.add(new SignalDeclaration(hdlExtPortSrcSgnl))
        val hdlBl = new BasedLiteral("(" + (tgtSgnlWidth - 1) + " downto " + srcSgnlWidth + " => " +
          hdlPortSrcSgnl.getIdentifier + "(" + (srcSgnlWidth - 1) + "))")
        arch.getStatements.add(new ConditionalSignalAssignment(hdlExtPortSrcSgnl, new Concatenate(hdlBl,
          hdlPortSrcSgnl)))
        (hdlPortTgtSgnl, hdlExtPortSrcSgnl)
      } else if (tgtSgnlWidth < srcSgnlWidth) {
        val hdlSlicedPortSrcSgnl = new Signal(hdlPortSrcSgnl.getIdentifier + "_" + (tgtSgnlWidth - 1) +
          "_downto_" + 0, StdLogic1164.STD_LOGIC_VECTOR(tgtSgnlWidth))
        arch.getDeclarations.add(new SignalDeclaration(hdlSlicedPortSrcSgnl))
        arch.getStatements.add(new ConditionalSignalAssignment(hdlSlicedPortSrcSgnl, hdlPortSrcSgnl.getSlice(new Range(new DecimalLiteral(tgtSgnlWidth-1), Range.Direction.DOWNTO, new DecimalLiteral(0)))))
        (hdlPortTgtSgnl, hdlSlicedPortSrcSgnl)
      } else {
        (hdlPortTgtSgnl, hdlPortSrcSgnl)
      }
    }

    def getCompInst(n: Node, nodeParams: Map[String, Any], hdlEntity: Entity, hdlfile: VhdlFile) = {

      /////////// declare the node components
      val hdlDeclComps = VhdlCollections.getAll(arch.getDeclarations, classOf[Component])
      val hdlDeclCompIds = hdlDeclComps.map(el => el.getIdentifier)
      val hdlComp = new Component(hdlEntity)
      /////////// add a component instance
      //  declared component entity if not already
      if (!hdlDeclCompIds.contains(hdlComp.getIdentifier)) {
        arch.getDeclarations.add(hdlComp)
        // Write Component VHDL file output to the directory
        VhdlOutput.toFile(hdlfile, outdir + hdlComp.getIdentifier + ".vhd")
      }

      val hdlInst: ComponentInstantiation = new ComponentInstantiation(n.inst, hdlComp)
      // Map Generics for the rest of the Nodes
      var hdlGenAssEls: List[AssociationElement] = List()
      val hdlGenParams = hdlEntity.getGeneric.toList.distinct.map(_.getVhdlObjects.get(0))
      for (hdlGenParam <- hdlGenParams) {
        if (!nodeParams.isEmpty) {
          val genericVal = nodeParams.get(hdlGenParam.getIdentifier).get
          val hdlGenAssEl = new AssociationElement(hdlGenParam.getIdentifier, hdlLiteralVal(genericVal))
          hdlGenAssEls = hdlGenAssEls ::: List(hdlGenAssEl)
          hdlInst.getGenericMap.add(hdlGenAssEl)
        }
      }
      // Map Generics Signals
      val hdlPortSgnls = hdlEntity.getPort.toList.distinct.map(_.getVhdlObjects.get(0))

      // input signals
      (grph get n).incoming.foreach { e =>
        // determine a mapped signal from a source node
        val srcNode = e.source.value
        val snkNode = e.target.value
        val snkPort = e.snkPort
        val srcPort = e.srcPort
        val srcNodeIdTup =
          srcNode match {
            case compNode: CompNode => {
              val hdlPortTgtSgnlOpt = hdlPortSgnls.find(s => s.getIdentifier.equals(snkPort.name))
              val hdlPortTgtSgnl = hdlPortTgtSgnlOpt.get
              val sgnlTyp = if (srcPort.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(srcPort.width) //getSgnlType(hdlGenAssEls, hdlPortTgtSgnl, srcPort.width)
              val hdlPortSrcSgnl = new Signal(srcNode.inst + "_" + srcPort.name, sgnlTyp)
              arch.getDeclarations.add(new SignalDeclaration(hdlPortSrcSgnl))
              // srcNodeIdTup = (1=port formal signal, 2=port actual signal, 3=index of this Port, 4= slice)
              (hdlPortTgtSgnl, hdlPortSrcSgnl, srcPort.index, srcPort.slice)
            }
            case tmplNode: TmplNode => {
              val hdlPortTgtSgnlOpt = hdlPortSgnls.find(s => s.getIdentifier.equals(snkPort.name))
              val hdlPortTgtSgnl = hdlPortTgtSgnlOpt.get
              val sgnlTyp = if (srcPort.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(srcPort.width) //getSgnlType(hdlGenAssEls, hdlPortTgtSgnl, srcPort.width)
              val hdlPortSrcSgnl = new Signal(srcNode.inst + "_" + srcPort.name, sgnlTyp)
              arch.getDeclarations.add(new SignalDeclaration(hdlPortSrcSgnl))
              // srcNodeIdTup = (1=port formal signal, 2=port actual signal, 3=index of this Port, 4= slice)
              (hdlPortTgtSgnl, hdlPortSrcSgnl, srcPort.index, srcPort.slice)
            }
            case _ => {
              val hdlPortTgtSgnlOpt = hdlPortSgnls.find(s => s.getIdentifier.equals(snkPort.name))
              val hdlPortTgtSgnl = hdlPortTgtSgnlOpt.get
              val sgnlTyp = if (srcPort.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(srcPort.width) //getSgnlType(hdlGenAssEls, hdlPortTgtSgnl, srcPort.width)
              val hdlPortSrcSgnl = new Signal(srcNode.inst, sgnlTyp)
              // srcNodeIdTup = (1=port formal signal, 2=port actual signal, 3=index of this Port, 4= slice)
              (hdlPortTgtSgnl, hdlPortSrcSgnl, srcPort.index, srcPort.slice)
            }

          }

        // map signals here
        val hdlPortTgtSgnl = srcNodeIdTup._1
        val hdlPortSrcSgnl = srcNodeIdTup._2
        val srcSgnlIndex: Int = srcNodeIdTup._3.toInt
        val srcSgnlSlice: (Int,Int) = srcNodeIdTup._4

        val hdlAssElIds = VhdlCollections.getAll(hdlInst.getPortMap, classOf[AssociationElement]).map { el =>
          (el.getFormal, el.getActual.asInstanceOf[Signal].getIdentifier)
        }
        val hdlAssEl = new AssociationElement(hdlPortTgtSgnl.getIdentifier, hdlPortSrcSgnl)
        if (!hdlAssElIds.contains((hdlAssEl.getFormal, hdlAssEl.getActual.asInstanceOf[Signal].getIdentifier))) {
          if (srcSgnlIndex == -1) {
            val formalWidth = getCompSgnlGenericVal(hdlGenAssEls, hdlPortTgtSgnl)
            val actualWidth = getSgnlMsbVal(hdlPortSrcSgnl)
            if (actualWidth > -1) {
              val assocElTup = getAssocElTup(hdlPortSrcSgnl, formalWidth, hdlPortTgtSgnl, actualWidth)
              hdlInst.getPortMap.add(new AssociationElement(assocElTup._1.getIdentifier, assocElTup._2))
            }
          }else if(srcSgnlSlice != null) { // Mapped element is std_logic and source is slice element
            val formalWidth = getCompSgnlGenericVal(hdlGenAssEls, hdlPortTgtSgnl)
            val actualWidth = getSgnlMsbVal(hdlPortSrcSgnl)
            val hdlSlicedPortSrcSgnl = new Signal((srcSgnlSlice._1-1) + "_" + (actualWidth - 1) +
              "_downto_" + 0, StdLogic1164.STD_LOGIC_VECTOR(actualWidth))
            arch.getDeclarations.add(new SignalDeclaration(hdlSlicedPortSrcSgnl))
            arch.getStatements.add(new ConditionalSignalAssignment(hdlSlicedPortSrcSgnl, hdlPortSrcSgnl.getSlice(new Range(new DecimalLiteral(srcSgnlSlice._1-1), Range.Direction.DOWNTO, new DecimalLiteral(srcSgnlSlice._2)))))
            hdlInst.getPortMap.add(new AssociationElement(hdlPortTgtSgnl.getIdentifier, hdlSlicedPortSrcSgnl))
          } else { // Mapped element is std_logic and source is array element
            val hdlIndexedMappedSgnl = new Signal(hdlPortSrcSgnl.getIdentifier + "_" + srcSgnlIndex,
              StdLogic1164.STD_LOGIC)
            arch.getDeclarations.add(new SignalDeclaration(hdlIndexedMappedSgnl))
            arch.getStatements.add(new ConditionalSignalAssignment(hdlIndexedMappedSgnl, hdlPortSrcSgnl
              .getArrayElement(srcSgnlIndex)))
            hdlInst.getPortMap.add(new AssociationElement(hdlPortTgtSgnl.getIdentifier, hdlIndexedMappedSgnl))
          }
        }
      }

      val hdlTmplAssElIds = VhdlCollections.getAll(hdlInst.getPortMap, classOf[AssociationElement]).map { el =>
        (el.getFormal, el.getActual.asInstanceOf[Signal].getIdentifier)
      }
      val hdlTmplInPortSgnls = hdlPortSgnls.filter(_.getMode == Mode.IN)

      // output signals
      (grph get n).outgoing.foreach { e =>
        // determine a mapped signal from a source node
        val snkNode = e.target.value
        val srcNode = e.source.value
        //val snkPort = e.snkPort
        val srcPort = e.srcPort

        val hdlPortTgtSgnlOpt = hdlPortSgnls.find(s => s.getIdentifier.equals(srcPort.name))
        val hdlPortTgtSgnl = hdlPortTgtSgnlOpt.get
        val sgnlTyp = if (srcPort.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(srcPort.width) //getSgnlType(hdlGenAssEls, hdlPortTgtSgnl, srcPort.width)
        val hdlPortSrcSgnl = new Signal(srcNode.inst + "_" + srcPort.name, sgnlTyp)
        arch.getDeclarations.add(new SignalDeclaration(hdlPortSrcSgnl))

        val hdlAssElIds = VhdlCollections.getAll(hdlInst.getPortMap, classOf[AssociationElement]).map { el =>
          (el.getFormal, el.getActual.asInstanceOf[Signal].getIdentifier)
        }
        val hdlAssEl = new AssociationElement(hdlPortTgtSgnl.getIdentifier, hdlPortSrcSgnl)

        if (!hdlAssElIds.contains((hdlAssEl.getFormal, hdlAssEl.getActual.asInstanceOf[Signal].getIdentifier))) {
          hdlInst.getPortMap.add(hdlAssEl)
        }
      }

      // assign port signals that are not in the graph incoming edges
      hdlTmplInPortSgnls.foreach { s =>
        if (!hdlTmplAssElIds.map(_._1).contains(s.getIdentifier)) {
          val hdlAssEl = new AssociationElement(s.getIdentifier, s)
          hdlInst.getPortMap.add(hdlAssEl)
          // add the signal to a entity port if not already added
          val hdlTmplPortSgnls = entity.getPort.toList.distinct.map(_.getVhdlObjects.get(0))
          val hdlTmplPortSgnlIds = hdlTmplPortSgnls.map(_.getIdentifier)
          if (!hdlTmplPortSgnlIds.contains(s.getIdentifier)) {
            entity.getPort.add(s)
          }
        }
      }

      hdlInst
    }

    nodes.foreach { n =>
      n match {
        // Stream node
        case strmNode: StrmNode => {
          // add node to input/output entity port
          if (roots.contains(strmNode))
            entity.getPort.add(new Signal(strmNode.name, Mode.IN, if (strmNode.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(strmNode.width)))
          else if (leaves.contains(strmNode))
            entity.getPort.add(new Signal(strmNode.name, Mode.OUT, if (strmNode.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(strmNode.width)))

          // incoming links
          (grph get strmNode).incoming.foreach { e =>
            e.source.value match {
              case compNode: CompNode => {
                val sgnlTyp = if (strmNode.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(strmNode.width)
                val hdlPortTgtSgnl = new Signal(strmNode.inst, sgnlTyp)
                val hdlPortSrcSgnl = new Signal(e.source.inst + "_" + e.srcPort.name, sgnlTyp)
                val srcWidth = e.srcPort.width
                val assocElTup = getAssocElTup(hdlPortSrcSgnl, srcWidth, hdlPortTgtSgnl, strmNode.width)
                arch.getStatements.add(new ConditionalSignalAssignment(assocElTup._1, assocElTup._2))
              }
              case tmplNode: TmplNode => {
                val sgnlTyp = if (strmNode.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(strmNode.width)
                val hdlPortTgtSgnl = new Signal(strmNode.inst, sgnlTyp)
                val hdlPortSrcSgnl = new Signal(e.source.inst + "_" + e.srcPort.name, sgnlTyp)
                val srcWidth = e.srcPort.width
                val assocElTup = getAssocElTup(hdlPortSrcSgnl, srcWidth, hdlPortTgtSgnl, strmNode.width)
                arch.getStatements.add(new ConditionalSignalAssignment(assocElTup._1, assocElTup._2))
              }
              case _ => {
                val sgnlTyp = if (strmNode.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(strmNode.width)
                val hdlPortTgtSgnl = new Signal(strmNode.inst, sgnlTyp)
                val hdlPortSrcSgnl = new Signal(e.source.inst, sgnlTyp)
                val srcWidth = e.source.width
                val assocElTup = getAssocElTup(hdlPortSrcSgnl, srcWidth, hdlPortTgtSgnl, strmNode.width)
                arch.getStatements.add(new ConditionalSignalAssignment(assocElTup._1, assocElTup._2))
              }
            }
          }
        }
        // Arithmetic node
        case arithNode: ArithNode => {
          (arithNode.lhs, arithNode.rhs) match {
            case (stmLhs: Streamer, stmRhs: Streamer) => {
              val csa = getCSA(arithNode, stmLhs.name, stmRhs.name)
              arch.getDeclarations.add(new SignalDeclaration(csa.getTarget.asInstanceOf[Signal]))
              arch.getStatements.add(csa)
            }
            case (stmLhs: Streamer, cmpRhs:  CompExp.Component) => {

              val inEdges = (dfg get n).incoming
              val srcNodeList = if (!inEdges.isEmpty) inEdges.toList.map(_.source).toList else List()

              val rhsId = if (!cmpRhs.isInstanceOf[ConstVal]) {
                getPredNodeLabel(arithNode, srcNodeList.filter(_.inst.equals(arithNode.rhs.getLastNodeId)).head)
              } else {
                val constVal = cmpRhs.asInstanceOf[ConstVal]
                toBinary(constVal.vl, constVal.width)
              }

              val csa = getCSA(arithNode, stmLhs.name, rhsId)
              arch.getDeclarations.add(new SignalDeclaration(csa.getTarget.asInstanceOf[Signal]))
              arch.getStatements.add(csa)
            }
            case (cmpLhs: CompExp.Component, cmpRhs: CompExp.Component) => {
              val inEdges = (dfg get n).incoming
              val srcNodeList = if (!inEdges.isEmpty) inEdges.toList.map(_.source).toList else List()

              val lhsId = if (!cmpLhs.isInstanceOf[ConstVal]) {
                getPredNodeLabel(arithNode, srcNodeList.filter(_.inst.equals(arithNode.lhs.getLastNodeId)).head)
              } else {
                val constVal = cmpLhs.asInstanceOf[ConstVal]
                toBinary(constVal.vl, constVal.width)
              }
              val rhsId = if (!cmpRhs.isInstanceOf[ConstVal]) {
                getPredNodeLabel(arithNode, srcNodeList.filter(_.inst.equals(arithNode.rhs.getLastNodeId)).head)
              } else {
                val constVal = cmpRhs.asInstanceOf[ConstVal]
                toBinary(constVal.vl, constVal.width)
              }
              val csa = getCSA(arithNode, lhsId, rhsId)
              arch.getDeclarations.add(new SignalDeclaration(csa.getTarget.asInstanceOf[Signal]))
              arch.getStatements.add(csa)
            }
            case (arthLhs: Arith2Arg, arthRhs: Arith2Arg) => {
              val csa = getCSA(arithNode, arthLhs.getLastNodeId, arthRhs.getLastNodeId)
              arch.getDeclarations.add(new SignalDeclaration(csa.getTarget.asInstanceOf[Signal]))
              arch.getStatements.add(csa)
            }
            // case _ =>
          }
        }
        // Logical node
        case logiNode: LogiNode => {
          if (!logiNode.lhs.equals(logiNode.rhs)) {
            (logiNode.lhs, logiNode.rhs) match {
              case (stmLhs: Streamer, stmRhs: Streamer) => {
                val csa = getCSA(logiNode, stmLhs.name, stmRhs.name)
                arch.getDeclarations.add(new SignalDeclaration(csa.getTarget.asInstanceOf[Signal]))
                arch.getStatements.add(csa)
              }
              case (stmLhs: Streamer, logiRhs: Logi2Arg) => {
                val csa = getCSA(logiNode, stmLhs.name, logiRhs.getLastNodeId)
                arch.getDeclarations.add(new SignalDeclaration(csa.getTarget.asInstanceOf[Signal]))
                arch.getStatements.add(csa)
              }
              case (arthLhs: Arith2Arg, arthRhs: Arith2Arg) => {
                val csa = getCSA(logiNode, arthLhs.getLastNodeId, arthRhs.getLastNodeId)
                arch.getDeclarations.add(new SignalDeclaration(csa.getTarget.asInstanceOf[Signal]))
                arch.getStatements.add(csa)
              }
              case (cmpLhs: CompExp.Component, cmpRhs: CompExp.Component) => {
                val inEdges = (dfg get n).incoming
                val srcNodeList = if (!inEdges.isEmpty) inEdges.toList.map(_.source).toList else List()

                val lhsId = if (!cmpLhs.isInstanceOf[ConstVal]) {
                  getPredNodeLabel(logiNode, srcNodeList.filter(_.inst.equals(logiNode.lhs.getLastNodeId)).head)
                } else {
                  val constVal = cmpLhs.asInstanceOf[ConstVal]
                  toBinary(constVal.vl, constVal.width)
                }
                val rhsId = if (!cmpRhs.isInstanceOf[ConstVal]) {
                  getPredNodeLabel(logiNode, srcNodeList.filter(_.inst.equals(logiNode.rhs.getLastNodeId)).head)
                } else {
                  val constVal = cmpRhs.asInstanceOf[ConstVal]
                  toBinary(constVal.vl, constVal.width)
                }

                val csa = getCSA(logiNode, lhsId, rhsId)
                arch.getDeclarations.add(new SignalDeclaration(csa.getTarget.asInstanceOf[Signal]))
                arch.getStatements.add(csa)
              }
              // case _ =>
            }
          } else {
            // Unary operation (e.g. "Not" logical operation)
            val s = new Signal(logiNode.name, if (logiNode.width == 1) StdLogic1164.STD_LOGIC else StdLogic1164.STD_LOGIC_VECTOR(logiNode.width))
            arch.getDeclarations.add(new SignalDeclaration(s))
            arch.getStatements.add(vhdlPrimitives.getNotCSA(logiNode))
          }
        }
        // Component node
        case compNode: CompNode => {
          if (!compNode.comp.isInstanceOf[ConstVal]) {
            // hdl file template
            val hdlfile = compNode.vhdlfile
            val hdlEntity = VhdlCollections.getAll(hdlfile.getElements, classOf[Entity]).head
            val params = compNode.comp._params.map { case (k, v) => k.toUpperCase -> v }
            val hdlInst = getCompInst(n, params, hdlEntity, hdlfile)
            arch.getStatements.add(hdlInst)

            // vld signal
            val hdlVldSrcSgnl = new Signal(compNode.inst + "_vld", StdLogic1164.STD_LOGIC)
            arch.getDeclarations.add(new SignalDeclaration(hdlVldSrcSgnl))
            val hdlVldPortSgnl = new Signal("vld", StdLogic1164.STD_LOGIC)
            hdlInst.getPortMap.add(new AssociationElement(hdlVldPortSgnl.getIdentifier, hdlVldSrcSgnl))
          }
        }
        // Template node
        case tmplNode: TmplNode => {
          if (!tmplNode.comp.isInstanceOf[ConstVal]) {
            // hdl file template
            val hdlfile = tmplNode.vhdlfile
            val hdlEntity = VhdlCollections.getAll(hdlfile.getElements, classOf[Entity]).head
            val params = tmplNode.comp._params.map { case (k, v) => k.toUpperCase -> v }
            val hdlInst = getCompInst(n, params, hdlEntity, hdlfile)
            arch.getStatements.add(hdlInst)

            val hdlPortSgnls = hdlEntity.getPort.toList.distinct.map(_.getVhdlObjects.get(0))
            if(hdlPortSgnls.map(_.getIdentifier).contains("vld")) {
              val hdlVldSrcSgnl = new Signal(tmplNode.inst + "_vld", StdLogic1164.STD_LOGIC)
              arch.getDeclarations.add(new SignalDeclaration(hdlVldSrcSgnl))
              val hdlVldPortSgnl = new Signal("vld", StdLogic1164.STD_LOGIC)
              hdlInst.getPortMap.add(new AssociationElement(hdlVldPortSgnl.getIdentifier, hdlVldSrcSgnl))
            }
          }
        }
        //case _ =>
      }
    }
  }
}


