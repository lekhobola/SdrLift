package sdrlift.apps

import exp.KernelExp.Macro
import exp.PatternsExp.Chain
import scalax.collection.Graph
import sdrlift.graph.NodeFactory.PortTypeEnum
import sdrlift.model.{Actor, Channel}

object MacroApps {
  case class App(name: String) extends SdrApp {

    case class Src(inst: String) extends Macro {
      override val name: String = "source"
      addOutPort("dout", PortTypeEnum.DOUT, List((1, "1")))
    }

    case class Modulator(inst: String) extends Macro {

      override val name: String = "qammod"

      addInPort("din", PortTypeEnum.DIN, List((1, "0"), (1, "1"), (1, "0")))

      addOutPort("Iout", PortTypeEnum.DOUT, List((2, "0"), (1, "1")))
      addOutPort("Qout", PortTypeEnum.DOUT, List((2, "0"), (1, "1")))
    }

    case class ZeroPad(inst: String) extends Macro {

      override val name: String = "zeropad"

      addParam("dWidth", 16)
      addParam("iSamples", 48)
      addParam("padLength", 16)

      addInPort("Iin", PortTypeEnum.DIN, List((48, "1"), (17, "0")))
      addInPort("Qin", PortTypeEnum.DIN, List((48, "1"), (17, "0")))

      addOutPort("Iout", PortTypeEnum.DOUT, List((1, "0"), (64, "1")))
      addOutPort("Qout", PortTypeEnum.DOUT, List((1, "0"), (64, "1")))
    }

    case class InvFFT(inst: String) extends Macro {

      override val name: String = "r22sdf_fft_ifft_core"

      addParam("N", 64)
      addParam("DIN_WIDTH", 16)
      addParam("DOUT_WIDTH", 22)
      addParam("MODE", '1')

      addInPort("XSr", PortTypeEnum.DIN, List((64, "1"), (64, "0")))
      addInPort("XSi", PortTypeEnum.DIN, List((64, "1"), (64, "0")))

      addOutPort("XKr", PortTypeEnum.DOUT, List((64, "0"), (64, "1")))
      addOutPort("XKi", PortTypeEnum.DOUT, List((64, "0"), (64, "1")))
    }

    case class CyclicPrefix(inst: String) extends Macro {

      override val name: String = "cpadd"

      addParam("dWidth", 22)
      addParam("iSamples", 64)
      addParam("prefixLength", 16)

      addInPort("Iin", PortTypeEnum.DIN, List((64, "1"), (65, "0")))
      addInPort("Qin", PortTypeEnum.DIN, List((64, "1"), (65, "0")))

      addOutPort("Iout", PortTypeEnum.DOUT, List((49, "0"), (80, "1")))
      addOutPort("Qout", PortTypeEnum.DOUT, List((49, "0"), (80, "1")))
    }

    case class Snk(inst: String) extends Macro {
      override val name: String = "sink"
      addInPort("iin", PortTypeEnum.DIN, List((1, "1")))
      addInPort("qin", PortTypeEnum.DIN, List((1, "1")))
    }

    val (source, qam, zp, ifft, cp, sink) =
      (Src("sourceInst"), Modulator("qamInst"), ZeroPad("zeropadInst"), InvFFT("ifftInst"), CyclicPrefix("cpInst"), Snk("sinkInst"))

    // val c = Chain(source , qam hasLinks((0, 0), (1, 1)), zp hasLinks((0, 0), (1, 1)), ifft hasLinks((0, 0), (1, 1)), cp hasLinks((0, 0), (1, 1)), sink)

    //override implicit val sdfap: Graph[Actor, Channel] = c.sdfap

    //def dsgn = createDesign(name, 00.0007752)

    override implicit val sdfap: Graph[Actor, Channel] = null

    def dsgn = null
  }
}
