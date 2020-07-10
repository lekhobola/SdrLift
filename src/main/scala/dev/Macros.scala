package sdrlift.apps

import exp.KernelExp.Macro
import exp.NodeExp.Streamer
import exp.PatternsExp.Chain
import scalax.collection.Graph
import sdrlift.graph.NodeFactory.PortTypeEnum
import sdrlift.model.{Actor, Channel}

object Macros {


    case class Src(inst: String, w: Int) extends Macro {
      override val name: String = "source"

      val en = Streamer("en", 1, PortTypeEnum.EN)
      val din = Streamer("din", 0, w, null, List((1, "1")), PortTypeEnum.DIN)
      val vld = Streamer("vld", 1, PortTypeEnum.VLD)
      val dout = Streamer("dout", 0, w, null, List((1, "1")), PortTypeEnum.DOUT)
      _ports = List(en, din, vld, dout)
      _params= Map("dWidth" -> w)
    }

    case class Modulator(inst: String) extends Macro {
      override val name: String = "qammod"

      val en = Streamer("en", 1, PortTypeEnum.EN)
      val din = Streamer("din", 0, 4, null, List((1, "0"), (1, "1"), (1, "0")), PortTypeEnum.DIN)
      val vld = Streamer("vld", 1, PortTypeEnum.VLD)
      val iout = Streamer("iout", 0, 16, null, List((2, "0"), (1, "1")), PortTypeEnum.DOUT)
      val qout = Streamer("qout", 0, 16, null, List((2, "0"), (1, "1")), PortTypeEnum.DOUT)
      _ports = List(en, din, vld, iout, qout)
    }

    /*case class ZeroPad(inst: String, w: Int) extends Macro {
      override val name: String = "zeropad"

      addParam("dWidth", 16)
      addParam("iSamples", 48)
      addParam("padLength", 16)

      val en = Streamer("en", 1, PortTypeEnum.EN)
      val iin = Streamer("Iin", 0, w, null, List((48, "1"), (17, "0")), PortTypeEnum.DIN)
      val qin = Streamer("Qin", 0, w, null, List((48, "1"), (17, "0")), PortTypeEnum.DIN)
      val vld = Streamer("vld", 1, PortTypeEnum.VLD)
      val iout = Streamer("Iout", 0, w, null, List((1, "0"), (64, "1")), PortTypeEnum.DOUT)
      val qout = Streamer("Qout", 0, w, null, List((1, "0"), (64, "1")), PortTypeEnum.DOUT)
    }

    case class InvFFT(inst: String, w: Int) extends Macro {

      override val name: String = "r22sdf_fft_ifft_core"

      addParam("N", 64)
      addParam("DIN_WIDTH", 16)
      addParam("DOUT_WIDTH", 22)
      addParam("MODE", '1')

      val en = Streamer("en", 1, PortTypeEnum.EN)
      val xsr = Streamer("XSr", 0, w, null, List((64, "1"), (64, "0")), PortTypeEnum.DIN)
      val xsi = Streamer("XSi", 0, w, null, List((64, "1"), (64, "0")), PortTypeEnum.DIN)
      val vld = Streamer("vld", 1, PortTypeEnum.VLD)
      val xkr = Streamer("XKr", 0, w, null, List((64, "0"), (64, "1")), PortTypeEnum.DOUT)
      val xki = Streamer("XKi", 0, w, null, List((64, "0"), (64, "1")), PortTypeEnum.DOUT)
    }

    case class CyclicPrefix(inst: String, w: Int) extends Macro {

      override val name: String = "cpadd"

      addParam("dWidth", 22)
      addParam("iSamples", 64)
      addParam("prefixLength", 16)

      val en = Streamer("en", 1, PortTypeEnum.EN)
      val iin = Streamer("Iin", 0, w, null, List((64, "1"), (65, "0")), PortTypeEnum.DIN)
      val qin = Streamer("Qin", 0, w, null, List((64, "1"), (65, "0")), PortTypeEnum.DIN)
      val vld = Streamer("vld", 1, PortTypeEnum.VLD)
      val iout = Streamer("Iout", 0, w, null, List((49, "0"), (80, "1")), PortTypeEnum.DOUT)
      val qout = Streamer("Qout", 0, w, null, List((49, "0"), (80, "1")), PortTypeEnum.DOUT)
    } */

    case class Snk(inst: String, w: Int) extends Macro {
      override val name: String = "sink"

      val en = Streamer("en", 1, PortTypeEnum.EN)
      val iin = Streamer("iin", 0, w, null, List((1, "1")), PortTypeEnum.DIN)
      val qin = Streamer("qin", 0, w, null, List((1, "1")), PortTypeEnum.DIN)
      val vld = Streamer("vld", 1, PortTypeEnum.VLD)
      val iout = Streamer("iout", 0, w, null, List((1, "1")), PortTypeEnum.DOUT)
      val qout = Streamer("qout", 0, w, null, List((1, "1")), PortTypeEnum.DOUT)

      _ports = List(en, iin, qin, vld, iout, qout)
      _params= Map("dWidth" -> w)
    }

   /* val (source, qam, zp, ifft, cp, sink) =
      (Src("sourceInst"), Modulator("qamInst"), ZeroPad("zeropadInst"), InvFFT("ifftInst"), CyclicPrefix("cpInst"), Snk("sinkInst"))

     val c = Chain(source , qam hasLinks((0, 0), (1, 1)), zp hasLinks((0, 0), (1, 1)), ifft hasLinks((0, 0), (1, 1)), cp hasLinks((0, 0), (1, 1)), sink)

    //override implicit val sdfap: Graph[Actor, Channel] = c.sdfap

    //def dsgn = createDesign(name, 00.0007752)

    override implicit val sdfap: Graph[Actor, Channel] = null

    def dsgn = null */

}
