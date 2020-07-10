
import Jama.Matrix
import analysis.Benchmarks
import dev.kernels.CIC.CIC_1_Stages
import dev.kernels.FIR.{DIR_FIR_4_TAP, DIR_FIR_64_TAP}
import dev.kernels.FFT._
import de.upb.hni.vmagic.output.VhdlOutput
import dev.apps.OFDM._
import exp.AppExp.SdrApp
import exp.AppExp._
import exp.CompExp._
import exp.CompExp.{Combinational, Component, CyclicPrefixAdd, Delay, ZeroPad}
import exp.PatternsExp.{Chain, FoldR, ZipWith}
import exp.KernelExp.{Macro, Module}
import exp.NodeExp.Streamer
import scalax.collection.Graph
import sdrlift.dev.IR3Apps
import sdrlift.dev.Macros.{Modulator, Snk, Src}
import codegen.{VhdlTemplateCodeGen, VhdlToplevelCodeGen}
import sdrlift.graph.{DfgEdge, DfgNode}
import sdrlift.model.{Actor, Channel}
import sdrlift.model.Sdfap._
import sdrlift.graph.Dfg._

import scala.Predef
import scala.collection._
import scala.collection.mutable.{ArrayBuffer, Builder, ListBuffer}
import scala.collection.generic._
import scala.collection.immutable.VectorBuilder
import scala.collection.immutable.Map
import com.concurrentthought.cla._
import dev.apps.DDC.{Fmddc, Gsmddc}


object SdrLiftTest {

  def main(args: Array[String]): Unit = {
    val app  = Opt.string(
      name     = "application",
      flags    = Seq("-a", "--app", "--application"),
      help     = "SDR application to be generated.",
      requiredFlag = true)

    val hdl     = Opt.string(
      name      = "application",
      flags     = Seq("-h", "--hdl", "--hdlcreate"),
      help      = "HDL code generation.",
      requiredFlag = true)

    val compile = Opt.string(
      name      = "compile",
      flags     = Seq("-c", "--c", "--compileapp"),
      help      = "Compile the application HDL code with Xilinx ISE.",
      requiredFlag = true)

    val bench       = Opt.string(
      name          = "bench",
      flags         = Seq("-b", "--bench", "--benchmark"),
      help          = "Save the Xilinx benckmark results to .csv files.",
      requiredFlag  = true)

    val initialArgs = Args(
      "run-main SdrLift [options]",
      "Demonstrates the CLA API.",
      """Note that --application required.""",
      Seq(app, Args.quietFlag))

    val finalArgs: Args = initialArgs.process(args)
    process(finalArgs)

    /*val ddc = Fmddc("fmddc")
    // ddc.sdfap.drawGraph
    ddc.codeGen(ddc.sdfap.bufferSlots(ddc.sdfap.maxThr), 4)
    ddc.build */
    // ddc.sdfap.bufferSlots(ddc.sdfap.maxThr).map(e => println(e._1 + " -> " + e._2))
    // ddc.sdfap.repetition.map(e => println(e._1 + " -> " + e._2))
    // ddc.sdfap.edges.map(e => println(e.srcActor.inst + " -> " + e.snkActor.inst + " = PP: " + e.srcPort.ap + " CP: " + e.snkPort.ap))
    // println(ddc.sdfap.bufferSlots(ddc.sdfap.maxThr).map(_._2.size).toList.reduceLeft(_ + _))

    //val gsm = Gsmddc("gsmddc")
    //gsm.sdfap.drawGraph
    // gsm.codeGen(gsm.sdfap.bufferSlots(gsm.sdfap.maxThr), 4)
    //gsm.build

    /*val rx11a = Rx80211a("Rx80211a")
    val slots = rx11a.sdfap.bufferSlots(rx11a.sdfap.maxThr)
    rx11a.sdfap.drawGraph
    rx11a.codeGen(slots, 4)
    rx11a.build*/

    /*val tx22 = Tx80222("Tx80222")
    val slots = tx22.sdfap.bufferSlots(tx22.sdfap.maxThr)
    tx22.codeGen(slots, 4)
    tx22.build*/
    
    //val rx22 = Rx80222("Rx80222")
    //val slots = rx22.sdfap.bufferSlots(rx22.sdfap.maxThr)
    //rx22.codeGen(slots, 4)
    //rx22.build
    //rx22.sdfap.rank

    //val mimotx = MimoTx("MimoTx")
    //val slots = mimotx.sdfap.bufferSlots(mimotx.sdfap.maxThr)
    //mimotx.sdfap.drawGraph
    //mimotx.codeGen(slots, 4)
    //mimotx.build
    //mimotx.sdfap.rank
    //println(mimotx.sdfap.repetition)

    ///val mimorx = MimoRx("MimoRx")
    // val slots = mimorx.sdfap.bufferSlots(mimorx.sdfap.maxThr)
     // mimorx.sdfap.drawGraph
    // mimorx.codeGen(slots, 4)
    //mimorx.build
    //mimotx.sdfap.rank
    //println(mimotx.sdfap.repetition)

  }

  def process(args : Args)= {
    if (args.get("application") != None) {
      println(args.get("application").get)
      /*
      val tx = Tx80211a("tx80211a")
      val slots = tx.sdfap.bufferSlots(tx.sdfap.maxThr)
      tx.codeGen(slots, 4)
      tx.build
      */
    } else {
      // Print all the default values or those specified by the user.
      args.printValues()

      // Print all the values including repeats.
      args.printAllValues()

      // Repeat the "other" arguments (not associated with flags).
      println("\nYou gave the following \"other\" arguments: " +
        args.remaining.mkString(", "))
    }
  }
}
