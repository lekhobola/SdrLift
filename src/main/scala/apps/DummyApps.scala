package sdrlift.apps

import de.upb.hni.vmagic.output.VhdlOutput
import exp.CompExp.Delay
import exp.KernelExp.Module
import exp.PatternsExp.{Chain, FoldR, ZipWith}
import sdrlift.codegen.vhdl.VhdlKernelCodeGen

object DummyApps {

  // Block
  /*   val comp = Comp("complexMult") {
     val cmb1 = Combinational {
       val (ar, ai, br, bi) = (Streamer("ar", 8), Streamer("ai", 8), Streamer("br", 8), Streamer("bi", 8))
       val ar_br = ar * br
       val ai_bi = ai * bi
       val ai_br = ai * br
       val ar_bi = ar * bi
       val cr = ar_br - ai_bi
       val ci = ai_br + ar_bi
       :=(cr, ci)
     }
     :=(cmb1)
   }

  val compHdl = new VhdlCompCodeGen(comp).toplevel(null, null)
  VhdlOutput.print(compHdl) */

  /*  val comp = Comp("complexMult") {
    val seq1 = Sequential {
      val (a, b, c, d) = (Streamer("a", 8), Streamer("b", 8), Streamer("c", 8), Streamer("d", 8))
      val fsm = Fsm("myFsm") has(
        "one" using {
          val h = a / 2
          val f = a + b
          val if1 = If(a < b && b < h || a < 1) {
            val (s, t) = (Streamer("s", 8), Streamer("t", 8))
            val g = s + t
            :=(g)
          } els {
            val (v, w) = (Streamer("v", 8), Streamer("w", 8))
            val j = v - w
            :=(j)
          }
          :=(if1, h, f)
        } goto("two", "three") when((a < 1), (a < 10)),
        "two" using {
          val f = a - b
          :=(f)
        } goto "three" when (a < b),
        "three" using {
          val f = a * b
          :=(f)
        } goto "one" when (a < 9)
      ) starting "one"
      :=(fsm)
    }
    :=(seq1)
  }

  val compHdl = new VhdlCompCodeGen(comp).toplevel(null, null)
  VhdlOutput.print(compHdl) */

  //val paths = comp.getArithLeafNodes
  //paths.foreach(println)

  /* val comp = Comp("sampleBlock") {
    val seq1 = Sequential {
      val (a, b, c, d) = (Streamer("a", 8), Streamer("b", 8), Streamer("c", 8), Streamer("d", 8))

      val if1 = If(a < b) {
        val (e, f) = (Streamer("e", 8), Streamer("f", 8))
        val g = e + f
        val ifif1 = If(e < f) {
          val (s, t) = (Streamer("s", 8), Streamer("t", 8))
          val g = s + t
          :=(g)
        } els {
          val (v, w) = (Streamer("v", 8), Streamer("w", 8))
          val j = v - w
          :=(j)
        }
        :=(ifif1, g)
      } elsIf(c < d, {
        val (h, i) = (Streamer("h", 8), Streamer("i", 8))
        val j = h + i
        :=(j)
      }) elsIf(a < d, {
        val (x, y) = (Streamer("x", 8), Streamer("y", 8))
        val z = x + y
        val ifif1 = If(x < y) {
          val (e, f) = (Streamer("e", 8), Streamer("f", 8))
          val g = e + f
          :=(g)
        } els {
          val (h, i) = (Streamer("h", 8), Streamer("i", 8))
          val j = h + i
          :=(j)
        }
        :=(ifif1, z)
      }) els {
        val (h, i) = (Streamer("h", 8), Streamer("i", 8))
        val j = h + i
        val ifif1 = If(h < i) {
          val (s, t) = (Streamer("s", 8), Streamer("t", 8))
          val g = s + t
          :=(g)
        } els {
          val (h, i) = (Streamer("h", 8), Streamer("i", 8))
          val j = h + i
          :=(j)
        }
        :=(ifif1, j)
      }
      :=(if1)
    }
    :=(seq1)
  }

  val compHdl = new VhdlCompCodeGen(comp).toplevel(null, null)
  VhdlOutput.print(compHdl) */

  /* var cr = Streamer()
  var ci = Streamer()

  val compA = Comp("CompA") {
    val cmb1 = Combinational {
      val (ar, ai, br, bi) = (Streamer("ar", 8), Streamer("ai", 8), Streamer("br", 8), Streamer("bi", 8))
      val ar_br = ar * br
      val ai_bi = ai * bi
      val ai_br = ai * br
      val ar_bi = ar * bi
      cr = ar_br - ai_bi
      ci = ai_br + ar_bi
      :=(cr, ci)
    }
    :=(cmb1)
  } */

  /*
  val compB = Comp("CompB") {
    val cmb1 = Combinational {
      val (ar, ai) = (Streamer("ar", 8), Streamer("ai", 8))
      val x = ar / ai
      :=(x)
    }
    :=(cmb1)
  }

  println(compA.oports)
  cr ~> compB.i(Streamer("ar", 8))
  val compHdl = new VhdlCompCodeGen(compB).toplevel(null, null)
  VhdlOutput.print(compHdl) */

  /* case class ModSample(name: String) extends Module {

    case class CompA(inst: String) extends Component {
      val name = "CompA"
      var cr = Streamer()
      var ci = Streamer()
      val cmb1 = Combinational {
        val (ar, ai, br, bi) = (Streamer("ar", 8), Streamer("ai", 8), Streamer("br", 8), Streamer("bi", 8))
        val ar_br = ar * br
        val ai_bi = ai * bi
        val ai_br = ai * br
        val ar_bi = ar * bi
        cr = ar_br - ai_bi
        ci = ai_br + ar_bi
        :=(cr, ci)
      }
      override val params: Map[String, Any] = null`
      override val inPorts: List[Streamer] = null
      override val outPorts: List[Streamer] = null
      override val dfg = model(Seq(cmb1))
    }

    case class CompB(inst: String) extends Component {
      var name = "CompB"
      var ar = Streamer("ar", 8)
      var ai = Streamer("ai", 8)
      val cmb1 = Combinational {
        val x = ar / ai
        :=(x)
      }
      // override val params: Map[String, Any] = null
      override val params: Map[String, Any] = null
      override val inPorts: List[Streamer] = null
      override val outPorts: List[Streamer] = null
      override val dfg = model(Seq(cmb1)) //Comp("CompB", Seq(cmb1))
    }

    val cA = CompA("cA")
    val cB = CompB("cB")
    val chain = Chain(cA hasLinks(cA.cr ~> cB.ar, cA.ci ~> cB.ai), cB)

    override val dfg = chain.dfg
  }

  val modSample = ModSample("modsample")
  val compHdl = new VhdlKernelCodeGen("modsample", modSample.dfg).getHdlFile(null, null)
  VhdlOutput.print(compHdl) */


  /* case class ModSample(name: String) extends Module {
    val cA = Delay("dl1", 8, 8)
    val cB = Delay("dl2", 8, 8)
    val cC = Delay("dl3", 8, 8)
    val cD = Delay("dl4", 8, 8)
    //val chain = Chain(cA hasLinks (cA.dout ~> cB.din), cB)
    //val chain = Chain(cA hasLinks (cA.dout ~> cB.din), cB hasLinks (cB.dout ~> cC.din), cC)
    val chain = Chain(cA hasLinks (cA.dout ~> cB.din), cB hasLinks (cB.dout ~> cC.din), cC hasLinks (cC.dout ~> cD
    .din), cD)
    override implicit val dfg = chain.dfg
    //val reduce = Reduce(cA, cB)(_ + _)
    //val reduce = Reduce(cA, cB, cC)(_ + _)
    //val reduce = Reduce(cA, cB, cC, cD)(_ + _)
    implicit val width : Int = 8
    val zipwith = ZipWith(cA, cB)(ConstVal(12), ConstVal(-10))(_ + _)
    val model = Kernel("ModuleSample", zipwith.dfg)
  }

  val modSample = ModSample("modsample")
  val compHdl = new VhdlKernelCodeGen("modsample", modSample.model.dfg).getHdlFile(null, null)
  VhdlOutput.print(compHdl) */
  // DIRECT FIR filter
  /*case class ModSample(name: String) extends Module {
    val cA = Delay("dl1", 8, 1)
    val cB = Delay("dl2", 8, 1)
    val cC = Delay("dl3", 8, 1)
    val cD = Delay("dl4", 8, 1)

    val chain = Chain(cA hasLinks (cA.dout ~> cB.din),
      cB hasLinks (cB.dout ~> cC.din),
      cC hasLinks (cC.dout ~> cD.din), cD)

    implicit var g = chain.dfg
    implicit val width: Int = 9
    val zw = ZipWith(cA, cB, cC, cD)(124, 214, 57, -33)(_ * _)
    val zwc = zw.comps
    g = zw.dfg
    val rd = Reduce(zwc(0), zwc(1), zwc(2), zwc(3))(_ + _)

    val model = Kernel("nfir", rd.dfg)
    override val dfg = null
  }
  val modSample = ModSample("nfir")
  //println(modSample.rd.dfg.order)
  //modSample.rd.dfg.nodes.foreach(println)
  val compHdl = new VhdlKernelCodeGen("nfir", modSample.model.dfg).getHdlFile(null, null)
  VhdlOutput.print(compHdl) */

  // DIRECT FIR filter
  /* case class ModSample(inst: String) extends Module {
    val cA = Delay("dl1", 8, 1)
    val cB = Delay("dl2", 8, 1)
    val cC = Delay("dl3", 8, 1)
    val cD = Delay("dl4", 8, 1)

    val chain = Chain(cA hasLinks (cA.dout ~> cB.din),
      cB hasLinks (cB.dout ~> cC.din),
      cC hasLinks (cC.dout ~> cD.din), cD)

    implicit var g = chain.dfg
    implicit val width: Int = 9
    val zw = ZipWith(cA, cB, cC, cD)(124, 214, 57, -33)(_ * _)
    val zwc = zw.comps
    g = zw.dfg
    val rd = Reduce(zwc(0), zwc(1), zwc(2), zwc(3))(_ + _)

    override val dfg = rd.dfg
    override val name: String = "nfir"
  }
  val modSample = ModSample("fir1")
  val compHdl = new VhdlKernelCodeGen("nfir", modSample.dfg).getHdlFile(null, null)
  VhdlOutput.print(compHdl) */

  /* case class App(name: String) extends Design {

    case class xifc(inst: String) extends Macro{
      override val name: String = "x"
      addParam("width", 8)
      addOutPort("dout", PortTypeEnum.DOUT, List((1, "011")))
    }
    case class yifc(inst: String) extends Macro{
      override val name: String = "y"
      addParam("width", 8)
      addInPort("din", PortTypeEnum.DIN, List((1, "10101")))
    }

    val x = xifc("xInst")
    val y = yifc("yInst")
    val c = Chain(x,y)

    override implicit val sdfap: Graph[Actor, Channel] = c.sdfap
    def dsgn  = createDesign(name, 0.5)
  }

  val app  = App("Sample")
  app.dsgn */

  /* case class Mixer(inst: String, w: Int) extends Component {
       // input nodes
       val x1 = Streamer("x1", w)
       val x2 =  Streamer("x2", w)
       // output node
       var y = Streamer()
       val cmb = Combinational {
         // arithmetic node
         y = x1 * x2
         :=(y) // return nodes
       }
       override val name = "Mixer"
       // generic parameters
       override val params = Map("width" -> w)
       // dfg for combinational nodes
       override val dfg = model(Seq(cmb))
     }*/

  /*   case class Fmddc(name: String) extends SdrApp {

  /* custom kernels here (omitted for brevity)... */
  // -- Mixer in Listing Listing 1
  // -- Compensation FIR as defined in Listing 2

  /* blackbox kernels interfaces */
  // FMC150 IP core
  case class ADC(inst: String) extends Macro {
    override val name: String = "fmc150_core"
    val dout = Port("cha_dout", PortTypeEnum.DOUT, AccessPatten((1, "1")))
  }
  // Numerically-controlled Oscillator (NCO) IP core
  case class NCO(inst: String) extends Macro {
    override val name: String = "nco_core"
    addParam("PHASE_WIDTH", 32)
    val cos = Port("cos_dout", PortTypeEnum.DOUT, AccessPatten((1, "1")))
    val sin = Port("sin_dout", PortTypeEnum.DOUT, AccessPatten((1, "1")))
  }
  // Cascaded Integrator Comb IP core
  case class CIC(inst: String) extends Macro {
    override val name: String = "cic_core"
    addParam("NUMBER_OF_STAGES", 4)
    addParam("DIFFERENTIAL_DELAY", 1)
    addParam("SAMPLE_RATE_CHANGE", 128)
    val din = Port("cos_dout", PortTypeEnum.DIN, AccessPatten((128, "1")))
    val dout = Port("sin_dout", PortTypeEnum.DOUT, AccessPatten((127, "0"), (1, "1")))
  }
  // Gigabit Ethernet IP core
  case class GbE(inst: String) extends Macro {
    override val name: String = "gbe_core"
    addParam("TX_BYTES_LENGTH", 128)
    val din_i = Port("udp_tx_pkt_data_i", PortTypeEnum.DIN, AccessPatten((1, "1")))
    val din_q = Port("udp_tx_pkt_data_q", PortTypeEnum.DIN, AccessPatten((1, "1")))
  }
  // declare kernels
  val (adc, nco, mixer_i, mixer_q, cic_i, cic_q, cfir_i, cfir_q, gbe) =
    (ADC("adc_Inst"), NCO("nco_Inst"), Mixer("mixeri_Inst", 16), Mixer("mixerq_Inst", 16), CIC("cici_Inst"), CIC("cicq_Inst"), CFIR("cici_Inst"), CFIR("cicq_Inst"), GbE("gbeInst"))

  // phase chain
  val chain_i = Chain(adc hasLinks((adc.dout ~> mixer_i.x1)), mixer_i hasLinks((mixer_i.y ~> cic_i.din)), cic_i hasLinks((cic_i.dout ~> cfir_i.x)), cfir_i hasLinks((cfir_i.y) ~> gbe.din_i)), gbe)
  // quadrature chain
  val chain_q = Chain(adc hasLinks((adc.dout ~> mixer_q.x1)), mixer_i hasLinks((mixer_q.y ~> cic_q.din)), cic_q hasLinks((cic_q.dout ~> cfir_q.x)), cfir_q hasLinks((cfir_q.y) ~> gbe.din_q)), gbe)
  // broadcast that connect NCO to MIXERs
  val broadcast = Broadcast(nco hasLinks((nco.cos ~> mixer_i.x2), (nco.sin ~> mixer_q.x2)))(mixer_i, mixer_q)
  // the SDF-AP model of the design
  override implicit val sdfap: Graph[Actor, Channel] = model(chain_i, chain_q, broadcast)
  // design with maximum throughput
  def design = createDesign("FM_Digital_Down_Converter", sdfap.max)
} */


  //val hdlgen = VhdlTemplateCodeGen("fifo", Map("width" -> 8, "depth" -> 2)).getVhdlCodeGen.getHdlFile(null, null)
  //VhdlOutput.print(hdlgen)

  //val hdlgen = VhdlTemplateCodeGen("mux2to1", Map("width" -> 8)).getVhdlCodeGen.getHdlFile()
  //VhdlOutput.print(hdlgen)
}
