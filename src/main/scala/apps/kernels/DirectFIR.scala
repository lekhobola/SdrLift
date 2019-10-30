package apps.kernels

import de.upb.hni.vmagic.output.VhdlOutput
import exp.CompExp.{Constants, Delay}
import exp.KernelExp.Module
import exp.NodeExp.Streamer
import exp.PatternsExp.{Chain, FoldR, ZipWith}
import sdrlift.codegen.vhdl.VhdlKernelCodeGen

import scala.collection.Seq

object DirectFIR {
  case class DFIR(inst: String, w: Int) extends Module {
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
}
