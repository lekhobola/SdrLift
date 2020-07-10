package apps.kernels

import de.upb.hni.vmagic.output.VhdlOutput
import exp.CompExp.{Constants, Delay}
import exp.KernelExp.Module
import exp.NodeExp.Streamer
import exp.PatternsExp.{Chain, FoldR, ZipWith}

import scala.collection.Seq

object FIR {

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
    override val input_length: Int = 1
    override val output_length: Int = 1
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
    //val chn = Chain(dly1, dly2, dly3, dly4, ...)
    val chn = Chain(
      dly1 outLinks (dly1.dout ~> dly2.din), dly2 outLinks (dly2.dout ~> dly3.din),
      dly3 outLinks (dly3.dout ~> dly4.din), dly4 outLinks (dly4.dout ~> dly5.din),
      dly5 outLinks (dly5.dout ~> dly6.din), dly6 outLinks (dly6.dout ~> dly7.din),
      dly7 outLinks (dly7.dout ~> dly8.din), dly8 outLinks (dly8.dout ~> dly9.din),
      dly9 outLinks (dly9.dout ~> dly10.din), dly10 outLinks (dly10.dout ~> dly11.din),
      dly11 outLinks (dly11.dout ~> dly12.din), dly12 outLinks (dly12.dout ~> dly13.din),
      dly13 outLinks (dly13.dout ~> dly14.din), dly14 outLinks (dly14.dout ~> dly15.din),
      dly15 outLinks (dly15.dout ~> dly16.din), dly16 outLinks (dly16.dout ~> dly17.din),
      dly17 outLinks (dly17.dout ~> dly18.din), dly18 outLinks (dly18.dout ~> dly19.din),
      dly19 outLinks (dly19.dout ~> dly20.din), dly20 outLinks (dly20.dout ~> dly21.din),
      dly21 outLinks (dly21.dout ~> dly22.din), dly22 outLinks (dly22.dout ~> dly23.din),
      dly23 outLinks (dly23.dout ~> dly24.din), dly24 outLinks (dly24.dout ~> dly25.din),
      dly25 outLinks (dly25.dout ~> dly26.din), dly26 outLinks (dly26.dout ~> dly27.din),
      dly27 outLinks (dly27.dout ~> dly28.din), dly28 outLinks (dly28.dout ~> dly29.din),
      dly29 outLinks (dly29.dout ~> dly30.din), dly30 outLinks (dly30.dout ~> dly31.din),
      dly31 outLinks (dly31.dout ~> dly32.din), dly32 outLinks (dly32.dout ~> dly33.din),
      dly33 outLinks (dly33.dout ~> dly34.din), dly34 outLinks (dly34.dout ~> dly35.din),
      dly35 outLinks (dly35.dout ~> dly36.din), dly36 outLinks (dly36.dout ~> dly37.din),
      dly37 outLinks (dly37.dout ~> dly38.din), dly38 outLinks (dly38.dout ~> dly39.din),
      dly39 outLinks (dly39.dout ~> dly40.din), dly40 outLinks (dly40.dout ~> dly41.din),
      dly41 outLinks (dly41.dout ~> dly42.din), dly42 outLinks (dly42.dout ~> dly43.din),
      dly43 outLinks (dly43.dout ~> dly44.din), dly44 outLinks (dly44.dout ~> dly45.din),
      dly45 outLinks (dly45.dout ~> dly46.din), dly46 outLinks (dly46.dout ~> dly47.din),
      dly47 outLinks (dly47.dout ~> dly48.din), dly48 outLinks (dly48.dout ~> dly49.din),
      dly49 outLinks (dly49.dout ~> dly50.din), dly50 outLinks (dly50.dout ~> dly51.din),
      dly51 outLinks (dly51.dout ~> dly52.din), dly52 outLinks (dly52.dout ~> dly53.din),
      dly53 outLinks (dly53.dout ~> dly54.din), dly54 outLinks (dly54.dout ~> dly55.din),
      dly55 outLinks (dly55.dout ~> dly56.din), dly56 outLinks (dly56.dout ~> dly57.din),
      dly57 outLinks (dly57.dout ~> dly58.din), dly58 outLinks (dly58.dout ~> dly59.din),
      dly59 outLinks (dly59.dout ~> dly60.din), dly60 outLinks (dly60.dout ~> dly61.din),
      dly61 outLinks (dly61.dout ~> dly62.din), dly62 outLinks (dly62.dout ~> dly63.din),
      dly63 outLinks (dly63.dout ~> dly64.din), dly64)
    // coefficients with data width set to 9
    val coeffs = Constants(9)(124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33, 124, 214, 57, -33)
    // zipwith for chain outputs and constant coefficients
    val zw = ZipWith(chn.comps)(coeffs)(_ * _)
    // apply a sum fold-right to zipwidth outputs
    val flr = FoldR(zw.comps)(_ + _)
    // x ~> (dly1, dly1.din; connects x to dly1, fld.out ~> y;  connects fld output to y
    override val dfg = model(Seq(x ~> (dly1, dly1.din), chn, zw, flr, flr.out ~> y))
    // filter module name */
    override val name: String = "nfir"
    override val input_length: Int = 1
    override val output_length: Int = 1
  }

}
