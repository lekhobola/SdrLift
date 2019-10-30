package sdrlift.model

import scalax.collection.GraphPredef._
import scalax.collection.GraphEdge._

import scala.collection.JavaConversions._
import scala.io.Source
import de.upb.hni.vmagic.`object`.Signal
import sdrlift.codegen.vhdl.VhdlFactory
import Sdfap._
import exp.KernelExp.KernelPort

import scala.collection.JavaConversions._

case class Channel[+N](srcActor: N, snkActor: N, id: String, srcPort: KernelPort, snkPort: KernelPort, dly: Int, portMap : (String,String)) //links : java.util.HashMap[String, (Actor,String)]
// DiEdge should be the base of any directed custom edge. Channel is the node type.
  extends DiEdge[N](NodeProduct(srcActor, snkActor))
    /**
      * If any of the label attributes is part of the key of the edge type,
      * ExtendedKey must be mixed in. An attribute is a key if it must be considered by equals.
      * 'id' is such a key attribute because there may exist several channels from and to the
      * same actor so we distinguished them by 'id'.
      */
    with ExtendedKey[N]
    // All edge implementations must mix in EdgeCopy.
    with EdgeCopy[Channel]
    //All edge implementations must mix in OuterEdge.
    with OuterEdge[N, Channel] {

  private def this(nodes: Product, id: String, srcPort: KernelPort, snkPort: KernelPort, dly: Int, portMap : (String,String)) {
    this(nodes.productElement(0).asInstanceOf[N],
      nodes.productElement(1).asInstanceOf[N], id, srcPort, snkPort, dly, portMap)
  }
  // Key attributes must be added to this Seq.
  def keyAttributes = Seq(id)
  /**
    * copy will be called by Graph transparently to create an inner edge. Thus copy plays the role
    * of an inner edge factory. It must return an instance of the edge class.
    */
  override def copy[NN](newNodes: Product) = new Channel[NN](newNodes, id, srcPort, snkPort, dly, portMap)

  // Establishes the Channel edge factory shortcut ## that propagates a directed edge to Channel.
  override protected def attributesToString = s" ($id)"

  //override def hashCode = id.toInt
  override def equals(other: Any) = other match {
    case that: Channel[N] => that.id == this.id
    case _                   => false
  }

  // code generation methods
  /////
  def componentLabel = "fifo"

  def path = /* System.getenv("DELITE_HOME") + */ "/home/lekhobola/Documents/dev/research/intelliJ-IDEA/workspace/hyperdsl/delite/" +
    "/dsls/sdrlift/src/uctsdrg/dsl/sdrlift/datastruct/scala/hdl/fifo/rtl/" + componentLabel + ".vhd"

  //def path = System.getenv("DELITE_HOME") + "/uctsdrg/dsl/sdrlift/datastruct/SdrLiftMainRunner/scala/datastructures/hdl/fifo/rtl/" + componentLabel + ".vhd"

  def template = new VhdlFactory(Source.fromFile(path).getLines.mkString)

  def signals = template.getSignals(true)

  def generics = template.getGenerics

  def component = template.getComponent(componentLabel)

  def instance(p:  java.util.HashMap[String,Any], dWidth: Int ) = template.getComponentInstantiation(componentLabel, id, p, wires(dWidth))

  def wires(dWidth: Int) =  template.getChannelWiresDecl(signals, id, dWidth)

  def wire(signal: String) =  template.getChannelWire(signal,id)

  def cpDecl(label: String, constVal: String) = template.getChannelCpDecl(label, constVal)

  def cp(label: String) = template.getChannelCp(label)
}

object Channel {

  case class FifoProps(ii: Int, cp: String, srcSchedulingPeriod: Int, snkSchedulingPeriod: Int, snkStartSchedule: Int,
                      shortedPredecessorSP: Int, size: Int, srcAp: List[Int], snkAp: List[Int], snkReadEnCtrl: List[Int]) {
    override def toString = "ii: " + ii + ", srcSchedulingPeriod: " + srcSchedulingPeriod + ", snkSchedulingPeriod: " +
      snkSchedulingPeriod + ", snkStartSchedule: " + snkStartSchedule + ", snkStartIndex: " + ", " +
      "shortedPredecessorSP: " + shortedPredecessorSP + ", size: " + size
  }

  /**
    * Declares the `Channel` edge factory shortcut `##` which can be invoked like
    * {{{
    * val (adder, mult) = (Actor("ADDER"), Actor("MULT"))
    * adder ~> mult ## "fifo_1" // yields Channel[Actor]
    * }}}
    */
  implicit final class ImplicitEdge[A <: Actor](val e: DiEdge[A]) extends AnyVal {
    def ##(id: String, srcPort: KernelPort, snkPort: KernelPort, dly: Int, portMap : (String,String)) = new Channel[A](e.source, e.target, id, srcPort, snkPort, dly, portMap)
  }
}