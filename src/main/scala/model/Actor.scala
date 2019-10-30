package sdrlift.model

import language.implicitConversions
import scala.io.Source
import de.upb.hni.vmagic.`object`.Signal
import scala.collection.JavaConversions._

import sdrlift.codegen.vhdl.VhdlFactory

/**
 * This object encapsulates the node and other helper types needed for
 * the channel example. The nodes of such a graph will be `Actor`s,
 * the edges `Actors`s.
 */
/* ------------------------------------------------- node type */
case class Actor(val id: String, val inst: String, val params: java.util.HashMap[String, Any], val execTime: Int) {

  override def toString = id + "," + inst + "," + params + ", " + execTime

  def label = inst + ":" + id

  val libdir = "iplib/"

  val path = libdir + id + "/rtl/" + id + ".vhd"

  //def path = System.getenv("DELITE_HOME") + "/uctsdrg/dsl/sdrlift/datastruct/SdrLiftMainRunner/scala/datastructures
  // /hdl/" + id + "/rtl/" + id + ".vhd"

  def template = new VhdlFactory(Source.fromFile(path).getLines.mkString)

  def signals = template.getSignals(true)

  def signalMode(s: String) = signals.filter { x => x.getIdentifier.equalsIgnoreCase(s) }.head.getMode

  def generics = template.getGenerics

  def component = template.getComponent(id)

  def instance(aMappedSignals: List[Signal]) =
    template.getComponentInstantiation(id, inst, params, aMappedSignals)

  def wires = template.getComponentWiresDecl(this, params)

  def wire(signal: String, labels: java.util.HashMap[String, String], label: String) =
    template.getComponentWire(signal, this, labels, label)
}
