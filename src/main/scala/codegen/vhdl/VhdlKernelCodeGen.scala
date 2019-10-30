package sdrlift.codegen.vhdl

import de.upb.hni.vmagic.{AssociationElement, Range, SubtypeDiscreteRange, VhdlFile, expression}
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
import de.upb.hni.vmagic.literal._
import NodeFactory._
import de.upb.hni.vmagic.`type`.IndexSubtypeIndication
import de.upb.hni.vmagic.expression.{Add => _, And => _, Or => _, _}
import de.upb.hni.vmagic.expression.{Aggregate => _, Equals => _, _}
import de.upb.hni.vmagic.output.VhdlOutput
import exp.KernelExp.Module

import scala.collection.immutable.Map
import scala.collection.JavaConversions._

case class VhdlKernelCodeGen(mod: Module) extends VhdlNodeCodeGen(mod)
