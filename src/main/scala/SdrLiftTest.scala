
import sdrlift.analysis.GrphAnalysis
import apps.kernels.DirectFIR.DFIR
import apps.kernels.FFT._
import sdrlift.apps.SdrApp
import de.upb.hni.vmagic.output.VhdlOutput
import exp.CompExp._
import exp.CompExp.{Combinational, Component, Delay}
import exp.PatternsExp.{Chain, FoldR, ZipWith}
import exp.KernelExp.{Macro, Module}
import exp.NodeExp.Streamer
import scalax.collection.Graph
import sdrlift.codegen.vhdl.{VhdlComponentCodeGen, VhdlKernelCodeGen, VhdlTemplateCodeGen}
import sdrlift.graph.{DfgEdge, DfgNode}
import sdrlift.model.{Actor, Channel}
import sdrlift.model.Sdfap._

import scala.Predef
import scala.collection._
import scala.collection.mutable.{ArrayBuffer, Builder, ListBuffer}
import scala.collection.generic._
import scala.collection.immutable.VectorBuilder
import scala.collection.immutable.Map

object SdrLiftTest {
  def main(args: Array[String]): Unit = {
    // val dfir = DFIR("nfir", 8)
    // val modHdl = new VhdlKernelCodeGen(dfir).getHdlFile(null, null)
    // VhdlOutput.print(modHdl)


    // val fft8 = FFT_N8("fft64", 16)
    // val modHdl = new VhdlKernelCodeGen(fft8).getHdlFile(null, null)
    // VhdlOutput.print(modHdl)

    val fft64 = FFT_N64("fft64", 16)
    val modHdl = new VhdlKernelCodeGen(fft64).getHdlFile(null, null)
    VhdlOutput.print(modHdl)

    /* val ifft64 = IFFT_N64("ifft64", 16)
    val modHdl = new VhdlKernelCodeGen(ifft64).getHdlFile(null, null)
    VhdlOutput.print(modHdl) */

    /* val fft2048 = FFT_N2048("fft2048", 16)
    val modHdl = new VhdlKernelCodeGen(fft2048).getHdlFile(null, null)
    VhdlOutput.print(modHdl) */
  }
}
