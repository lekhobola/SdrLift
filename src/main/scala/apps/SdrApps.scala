package sdrlift.apps

import de.upb.hni.vmagic.VhdlFile
import de.upb.hni.vmagic.output.VhdlOutput
import scalax.collection.Graph
import sdrlift.codegen.vhdl.VhdlToplevelCodeGen
import sdrlift.model.{Actor, Channel}
import sdrlift.model.Sdfap._

trait SdrApp {

  val sdfap: Graph[Actor, Channel]

  def createDesign(name: String, thr: Double)(implicit grph: Graph[Actor, Channel]) = {
    val slots = grph.bufferSlots(thr)
    val hdlgen = VhdlToplevelCodeGen(grph)
    val vhdlFile = null //hdlgen.toplevel(slots, name, null, null, 4)

    //VhdlOutput.print(vhdlFile._1)
  }
}
