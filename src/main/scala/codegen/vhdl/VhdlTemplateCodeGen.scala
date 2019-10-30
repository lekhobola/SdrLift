package sdrlift.codegen.vhdl

import sdrlift.codegen.vhdl.template._

import scala.collection.immutable.Map


case class VhdlTemplateCodeGen(tpName: String, params: Map[String, Any]) {
  /**
    * creates the VHDL file
    */
  def getVhdlCodeGen: VhdlCodeGen = tpName match {
    case "delay" => DelayTp(params)
    case "fifo" => FifoTp(params)
    case "mux2to1" => Mux2to1Tp(params)
    case "counter" => CounterTp(params)
    case "rom" => RomTp(params)
    case "rounder" => RounderTp(params)
    case _ => null
  }
}
