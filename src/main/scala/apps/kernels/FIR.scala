package apps.kernels

import de.upb.hni.vmagic.output.VhdlOutput
import exp.CompExp.{Constants, Delay}
import exp.KernelExp.Module
import exp.NodeExp.Streamer
import exp.PatternsExp.{Chain, FoldR, ZipWith}
import sdrlift.codegen.vhdl.VhdlKernelCodeGen

import scala.collection.Seq

object DirectFIR {

  case class DIR_FIR_4_TAP(inst: String, w: Int) extends Module {
    // input node x and output node y
    val (x, y) = (Streamer("x", w), Streamer("y", 2 * w + 1))
    override val iopaths = List((x, y))
    // declare delay templates
    val (dly1, dly2, dly3, dly4) = (Delay("dly1", w, 1), Delay("dly2", w, 1), Delay("dly3", w, 1), Delay("dly4", w, 1))
    // stitch up the delay template nodes
    //val chn = Chain(dly1, dly2, dly3, dly4)
    val chn = Chain(dly1 outLinks (dly1.dout ~> dly2.din), dly2 outLinks (dly2.dout ~> dly3.din), dly3 outLinks (dly3.dout ~> dly4.din), dly4)
    // coefficients with data width set to 9
    val coeffs = Constants(9)(124, 214, 57, -33)
    // zipwith for chain outputs and constant coefficients
    val zw = ZipWith(chn.comps)(coeffs)(_ * _)
    // apply a sum fold-right to zipwidth outputs
    val fld = FoldR(zw.comps)(_ + _)
    // x ~> (dly1, dly1.din; connects x to dly1, fld.out ~> y;  connects fld output to y
    override val dfg = model(Seq(x ~> (dly1, dly1.din), chn, zw, fld, fld.out ~> y))
    // filter module name
    override val name: String = "nfir"
  }

  /* val dfir = DFIR("nfir", 8)
  val compHdl = new VhdlKernelCodeGen(dfir).getHdlFile(null, null)
  VhdlOutput.print(compHdl) */


  case class DIR_FIR_64_TAP(inst: String, w: Int) extends Module {
    // input node x and output node y
    val (x, y) = (Streamer("x", w), Streamer("y", 2 * w + 1))
    override val iopaths = List((x, y))
    // declare delay templates
    val (dly1, dly2, dly3, dly4, dly5, dly6, dly7, dly8,
    dly9, dly10, dly11, dly12, dly13, dly14, dly15, dly16) =
      (Delay("dly1", w, 1), Delay("dly2", w, 1), Delay("dly3", w, 1), Delay("dly4", w, 1),
        Delay("dly5", w, 1), Delay("dly6", w, 1), Delay("dly7", w, 1), Delay("dly8", w, 1),
        Delay("dly9", w, 1), Delay("dly10", w, 1), Delay("dly11", w, 1), Delay("dly12", w, 1),
        Delay("dly13", w, 1), Delay("dly14", w, 1), Delay("dly15", w, 1), Delay("dly16", w, 1))

    val (dly17, dly18, dly19, dly20, dly21, dly22, dly23, dly24,
    dly25, dly26, dly27, dly28, dly29, dly30, dly31, dly32) = (
      Delay("dly17", w, 1), Delay("dly18", w, 1), Delay("dly19", w, 1), Delay("dly20", w, 1),
      Delay("dly21", w, 1), Delay("dly22", w, 1), Delay("dly23", w, 1), Delay("dly24", w, 1),
      Delay("dly25", w, 1), Delay("dly26", w, 1), Delay("dly27", w, 1), Delay("dly28", w, 1),
      Delay("dly29", w, 1), Delay("dly30", w, 1), Delay("dly31", w, 1), Delay("dly32", w, 1))

    val (dly33, dly34, dly35, dly36, dly37, dly38, dly39, dly40,
    dly41, dly42, dly43, dly44, dly45, dly46, dly47, dly48) =
      (Delay("dly33", w, 1), Delay("dly34", w, 1), Delay("dly35", w, 1), Delay("dly36", w, 1),
        Delay("dly37", w, 1), Delay("dly38", w, 1), Delay("dly39", w, 1), Delay("dly40", w, 1),
        Delay("dly41", w, 1), Delay("dly42", w, 1), Delay("dly43", w, 1), Delay("dly44", w, 1),
        Delay("dly45", w, 1), Delay("dly46", w, 1), Delay("dly47", w, 1), Delay("dly48", w, 1))

    val (dly49, dly50, dly51, dly52, dly53, dly54, dly55, dly56,
    dly57, dly58, dly59, dly60, dly61, dly62, dly63, dly64) =
      (Delay("dly49", w, 1), Delay("dly50", w, 1), Delay("dly51", w, 1), Delay("dly52", w, 1),
        Delay("dly53", w, 1), Delay("dly54", w, 1), Delay("dly55", w, 1), Delay("dly56", w, 1),
        Delay("dly57", w, 1), Delay("dly58", w, 1), Delay("dly59", w, 1), Delay("dly60", w, 1),
        Delay("dly61", w, 1), Delay("dly62", w, 1), Delay("dly63", w, 1), Delay("dly64", w, 1))
    // stitch up the delay template nodes
    //val chn = Chain(dly1, dly2, dly3, dly4)
    /* val chn = Chain(dly1 outLinks (dly1.dout ~> dly2.din), dly2 outLinks (dly2.dout ~> dly3.din), dly3 outLinks (dly3.dout ~> dly4.din), dly4)
    // coefficients with data width set to 9
    val coeffs = Constants(9)(124, 214, 57, -33)
    // zipwith for chain outputs and constant coefficients
    val zw = ZipWith(chn.comps)(coeffs)(_ * _)
    // apply a sum fold-right to zipwidth outputs
    val fld = FoldR(zw.comps)(_ + _)
    // x ~> (dly1, dly1.din; connects x to dly1, fld.out ~> y;  connects fld output to y
    override val dfg = model(Seq(x ~> (dly1, dly1.din), chn, zw, fld, fld.out ~> y))
    // filter module name */
    override val name: String = "nfir"
  }

}
