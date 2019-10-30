package sdrlift.codegen.vhdl

import de.upb.hni.vmagic.{AssociationElement, SubtypeDiscreteRange, VhdlFile, expression}
import de.upb.hni.vmagic.`object`.{AttributeExpression, Constant, Signal}
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
import de.upb.hni.vmagic.literal.{BasedLiteral, BinaryLiteral, DecimalLiteral}
import NodeFactory._
import de.upb.hni.vmagic.expression.{Add => _, And => _, Or => _, _}
import de.upb.hni.vmagic.expression.{Add, And, Divide, LessThan, Multiply, Or, Subtract}
import de.upb.hni.vmagic.output.VhdlOutput
import de.upb.hni.vmagic.util.VhdlCollections
import exp.CompExp.{Combinational, Sequential}
import scalax.collection.GraphTraversal.{BreadthFirst, DepthFirst, Predecessors}

import scala.collection.immutable.Map
//import de.upb.hni.vmagic.expression.{Add, Subtract, Divide, Or, And, Multiply, LessThan}
import de.upb.hni.vmagic.highlevel.StateMachine

import scala.collection.JavaConversions._

case class VhdlComponentCodeGen(cmp: exp.CompExp.Component) extends VhdlNodeCodeGen(cmp)
