package apps.kernels

import de.upb.hni.vmagic.output.VhdlOutput
import exp.CompExp.{:=, Combinational, Component, Counter, Delay, Mux2to1, Rom, Rounder}
import exp.KernelExp.Module
import exp.NodeExp.Streamer
import exp.PatternsExp.{Chain, FoldR, ZipWith}
import scalax.collection.Graph
import sdrlift.codegen.vhdl.{VhdlComponentCodeGen, VhdlKernelCodeGen}
import sdrlift.graph.{DfgEdge, DfgNode}

import scala.collection.Seq

object FFT {
  case class ComplexMult(inst: String, a_width: Int, b_width: Int, c_width: Int) extends Component {
    //inputs nodes
    val (ar, ai, br, bi) = (Streamer("ar", a_width), Streamer("ai", a_width), Streamer("br", b_width), Streamer("bi", b_width))
    // output nodes
    val (cr, ci) = (Streamer("cr", c_width), Streamer("ci", c_width))

    override val iopaths = List((ar, cr))

    // combinational logic
    val cmb = Combinational {
      // arithmetic nodes
      val ar_br = ar * br
      val ai_bi = ai * bi
      val ai_br = ai * br
      val ar_bi = ar * bi
      val ar_br_ai_bi_sub = ar_br - ai_bi
      val ai_br_ar_bi_sum = ai_br + ar_bi
      // return registers and output ports
      :=(ar_br_ai_bi_sub, ai_br_ar_bi_sum, cr, ci, ar_br_ai_bi_sub ~> cr, ai_br_ar_bi_sum ~> ci)
    }
    // component name
    override val name: String = inst + "_cmult"

    // directed flow graph (DFG)
    override def dfg: Graph[DfgNode, DfgEdge] = model(Seq(cmb))
  }

  /* val mult = ComplexMult("cmult", 18, 16, 34)
    val compHdl = new VhdlComponentCodeGen(mult).getHdlFile()
    VhdlOutput.print(compHdl)*/


  // Butterfly - 1
  case class BF2I(inst: String, w: Int) extends Component {
    //inputs
    val (s, xpr, xpi, xfr, xfi) = (Streamer("s", 1), Streamer("xpr", w), Streamer("xpi", w), Streamer("xfr", w + 1), Streamer("xfi", w + 1))
    // outputs
    val (znr, zni, zfr, zfi) = (Streamer("znr", w + 1), Streamer("zni", w + 1), Streamer("zfr", w + 1), Streamer("zfi", w + 1))

    override val iopaths = List((xpr, znr))

    val cmb = Combinational {
      val xfr_xpr_sum = xfr + xpr
      val xfi_xpi_sum = xfi + xpi
      val xfr_xpr_diff = xfr - xpr
      val xfi_xpi_diff = xfi - xpi

      val znr_mux = Mux2to1("znr_mux", w + 1)
      val znr_mux_comm = znr_mux inLinks(s ~> znr_mux.sel, xfr ~> znr_mux.din1, xfr_xpr_sum ~> znr_mux.din2) outLinks (znr_mux.dout ~> znr)

      val zni_mux = Mux2to1("zni_mux", w + 1)
      val zni_mux_comm = zni_mux inLinks(s ~> zni_mux.sel, xfi ~> zni_mux.din1, xfi_xpi_sum ~> zni_mux.din2) outLinks (zni_mux.dout ~> zni)

      val zfr_mux = Mux2to1("zfr_mux", w + 1)
      val zfr_mux_comm = zfr_mux inLinks(s ~> zfr_mux.sel, xpr ~> zfr_mux.din1, xfr_xpr_diff ~> zfr_mux.din2) outLinks (zfr_mux.dout ~> zfr)

      val zfi_mux = Mux2to1("zfi_mux", w + 1)
      val zfi_mux_comm = zfi_mux inLinks(s ~> zfi_mux.sel, xpi ~> zfi_mux.din1, xfi_xpi_diff ~> zfi_mux.din2) outLinks (zfi_mux.dout ~> zfi)

      :=(xfr_xpr_sum, xfi_xpi_sum, xfr_xpr_diff, xfi_xpi_diff, znr_mux_comm, zni_mux_comm, zfr_mux_comm, zfi_mux_comm)
    }

    override val name: String = inst + "_bf2i"
    override val width: Int = w + 1

    override def dfg: Graph[DfgNode, DfgEdge] = model(Seq(cmb))
  }

  /* val bf2i = BF2I("bf2iInst", 16)
  val compHdl = new VhdlComponentCodeGen(bf2i).getHdlFile()
  VhdlOutput.print(compHdl) */


  case class MUXim(id: String, w: Int) extends Component {
    //inputs
    val (cc, br, bi) = (Streamer("cc", 1), Streamer("br", w), Streamer("bi", w))
    // outputs
    val (zr, zi) = (Streamer("zr", w), Streamer("zi", w))

    override val iopaths = List((br, zr))

    val cmb = Combinational {
      val zr_mux = Mux2to1("zr_mux", w)
      val zr_mux_comm = zr_mux inLinks(cc ~> (zr_mux, zr_mux.sel), br ~> (zr_mux, zr_mux.din1), bi ~> (zr_mux, zr_mux.din2)) outLinks ((zr_mux, zr_mux.dout) ~> zr)
      val zi_mux = Mux2to1("zi_mux", w)
      val zi_mux_comm = zi_mux inLinks(cc ~> (zi_mux, zi_mux.sel), bi ~> (zi_mux, zi_mux.din1), br ~> (zi_mux, zi_mux.din2)) outLinks ((zi_mux, zi_mux.dout) ~> zi)
      :=(zr_mux_comm, zi_mux_comm)
    }
    override val name: String = id + "_MUXim"
    override val inst: String = name + "_Inst"
    override val width: Int = w

    override def dfg: Graph[DfgNode, DfgEdge] = model(Seq(cmb))
  }

  /* val muxim = MUXim("muximInst", 8)
  val compHdl = new VhdlComponentCodeGen(muxim).getHdlFile()
  VhdlOutput.print(compHdl) */

  case class MUXsg(id: String, w: Int) extends Component {
    //inputs
    val (cc, g1, g2) = (Streamer("cc", 1), Streamer("g1", w), Streamer("g2", w))
    // outputs
    val (h1, h2) = (Streamer("znr", w), Streamer("zni", w))

    override val iopaths = List((g1, h1))

    val cmb = Combinational {
      val g1_g2_sum = g1 + g2
      val g1_g2_diff = g1 - g2
      val muxim = MUXim(id + "_MUXsg", w)
      val muxim_comm = muxim inLinks(cc ~> muxim.cc, g1_g2_sum ~> muxim.br, g1_g2_diff ~> muxim.bi) outLinks(muxim.zr ~> h1, muxim.zi ~> h2)
      :=(g1_g2_sum, g1_g2_diff, muxim_comm)
    }
    override val name: String = id + "_MUXsg"
    override val inst: String = name + "_Inst"
    override val width: Int = w

    override def dfg: Graph[DfgNode, DfgEdge] = model(Seq(cmb))
  }

  /*val muxsg = MUXsg("muxsgInst", 8)
  val compHdl = new VhdlComponentCodeGen(muxsg).getHdlFile()
  VhdlOutput.print(compHdl)*/

  // Butterfly - 2
  case class BF2II(inst: String, w: Int) extends Component {
    //inputs
    val (s, t, xpr, xpi, xfr, xfi) = (Streamer("s", 1), Streamer("t", 1), Streamer("xpr", w), Streamer("xpi", w), Streamer("xfr", w + 1), Streamer("xfi", w + 1))
    // outputs
    val (znr, zni, zfr, zfi) = (Streamer("znr", w + 1), Streamer("zni", w + 1), Streamer("zfr", w + 1), Streamer("zfi", w + 1))

    override val iopaths = List((xpr, znr))

    val cmb = Combinational {
      val t_not = t !
      val cc = s && t_not;

      val muxim = MUXim(inst, w)
      val muxim_comm = muxim inLinks(cc ~> muxim.cc, xpr ~> muxim.br, xpi ~> (muxim, muxim.bi))

      val muxsg = MUXsg(inst, w + 1)
      val muxsg_comm = muxsg inLinks(cc ~> muxsg.cc, xfi ~> muxsg.g1, (muxim, muxim.zi) ~> muxsg.g2)

      val xfr_xpr_sum = xfr + (muxim, muxim.zr)
      val xfr_xpr_diff = xfr - (muxim, muxim.zr)

      val znr_mux = Mux2to1("znr_mux", w + 1)
      val znr_mux_comm = znr_mux inLinks(s ~> znr_mux.sel, xfr ~> znr_mux.din1, xfr_xpr_sum ~> znr_mux.din2) outLinks (znr_mux.dout ~> znr)

      val zni_mux = Mux2to1("zni_mux", w + 1)
      val zni_mux_comm = zni_mux inLinks(s ~> zni_mux.sel, xfi ~> zni_mux.din1, (muxsg, muxsg.h1) ~> zni_mux.din2) outLinks (zni_mux.dout ~> zni)

      val zfr_mux = Mux2to1("zfr_mux", w + 1)
      val zfr_mux_comm = zfr_mux inLinks(s ~> zfr_mux.sel, (muxim, muxim.zr) ~> zfr_mux.din1, xfr_xpr_diff ~> zfr_mux.din2) outLinks (zfr_mux.dout ~> zfr)

      val zfi_mux = Mux2to1("zfi_mux", w + 1)
      val zfi_mux_comm = zfi_mux inLinks(s ~> zfi_mux.sel, (muxim, muxim.zi) ~> zfi_mux.din1, (muxsg, muxsg.h2) ~> zfi_mux.din2) outLinks (zfi_mux.dout ~> zfi)

      :=(cc, muxim_comm, muxsg_comm, xfr_xpr_sum, xfr_xpr_diff, znr_mux_comm, zni_mux_comm, zfr_mux_comm, zfi_mux_comm)
    }

    override val name: String = inst + "_bf2ii"
    override val width: Int = w + 1

    override def dfg: Graph[DfgNode, DfgEdge] = model(Seq(cmb))
  }

  /*val bf2ii = BF2II("bf2iiInst", 17)
  val compHdl = new VhdlComponentCodeGen(bf2ii).getHdlFile()
  VhdlOutput.print(compHdl) */

  // val hdlgen = VhdlTemplateCodeGen("rom", Map("addr_width" -> 3, "data_width" -> 8, "vector" -> Seq(1,2,3,4,5,6))).getVhdlCodeGen.getHdlFile()
  // VhdlOutput.print(hdlgen)

  case class Stage(inst: String, data_width: Int, b1_depth: Int, b2_depth: Int) extends Component {

    //inputs
    val (en, s1, s2, tfr, tfi, dinr, dini) = (Streamer("en", 1), Streamer("s1", 1), Streamer("s2", 1), Streamer("tfr", 16), Streamer("tfi", 16), Streamer("dinr", data_width), Streamer("dini", data_width))
    // outputs
    val (doutr, douti) = (Streamer("doutr", data_width + 2), Streamer("douti", data_width + 2))

    override val iopaths = List((dinr, doutr))

    val cmb = Combinational {
      // BF2I
      val bf2i = BF2I(inst + "_bf2i", data_width)
      val bf2i_comm = bf2i inLinks(s1 ~> (bf2i, bf2i.s), dinr ~> (bf2i, bf2i.xpr), dini ~> (bf2i, bf2i.xpi))

      // BF2I shift registers
      val bf2i_sreg_r = Delay(inst + "_bf2i_sreg_r", data_width + 1, b1_depth)
      val bf2i_sreg_r_comm = bf2i_sreg_r inLinks(en ~> (bf2i_sreg_r.en), (bf2i, bf2i.zfr) ~> (bf2i_sreg_r, bf2i_sreg_r.din)) outLinks ((bf2i_sreg_r, bf2i_sreg_r.dout) ~> (bf2i, bf2i.xfr))
      val bf2i_sreg_i = Delay(inst + "_bf2i_sreg_i", data_width + 1, b1_depth)
      val bf2i_sreg_i_comm = bf2i_sreg_i inLinks(en ~> (bf2i_sreg_i.en), (bf2i, bf2i.zfi) ~> (bf2i_sreg_i, bf2i_sreg_i.din)) outLinks ((bf2i_sreg_i, bf2i_sreg_i.dout) ~> (bf2i, bf2i.xfi))

      // BF2II
      val bf2ii = BF2II(inst + "_bf2ii", data_width + 1)
      val bf2ii_comm = bf2ii inLinks(s2 ~> (bf2ii, bf2ii.s), s1 ~> (bf2ii, bf2ii.t), (bf2i, bf2i.znr) ~> (bf2ii, bf2ii.xpr), (bf2i, bf2i.zni) ~> (bf2ii, bf2ii.xpi))

      // BF2II shift registers
      val bf2ii_sreg_r = Delay(inst + "_bf2ii_sreg_r", data_width + 2, b2_depth)
      val bf2ii_sreg_r_comm = bf2ii_sreg_r inLinks(en ~> (bf2ii_sreg_r.en), (bf2ii, bf2ii.zfr) ~> (bf2ii_sreg_r, bf2ii_sreg_r.din)) outLinks ((bf2ii_sreg_r, bf2ii_sreg_r.dout) ~> (bf2ii, bf2ii.xfr))
      val bf2ii_sreg_i = Delay(inst + "_bf2ii_sreg_i", data_width + 2, b2_depth)
      val bf2ii_sreg_i_comm = bf2ii_sreg_i inLinks(en ~> (bf2ii_sreg_i.en), (bf2ii, bf2ii.zfi) ~> (bf2ii_sreg_i, bf2ii_sreg_i.din)) outLinks ((bf2ii_sreg_i, bf2ii_sreg_i.dout) ~> (bf2ii, bf2ii.xfi))

      // Multiplier
      val cmult = ComplexMult(inst, data_width + 2, 16, data_width + 18)
      val cmult_comm = cmult inLinks((bf2ii, bf2ii.znr) ~> (cmult, cmult.ar), (bf2ii, bf2ii.zni) ~> (cmult, cmult.ai), tfr ~> (cmult, cmult.br), tfi ~> (cmult, cmult.bi)) // outLinks((cmult, cmult.cr) ~> doutr, (cmult, cmult.ci) ~> douti)

      // Rounder
      val rounder_r = Rounder(inst + "_cmultr", data_width + 16, data_width + 2)
      val rounder_r_comm = rounder_r inLinks ((cmult, cmult.cr) ~> (rounder_r, rounder_r.din)) outLinks ((rounder_r, rounder_r.dout) ~> doutr)
      val rounder_i = Rounder(inst + "_cmulti", data_width + 16, data_width + 2)
      val rounder_i_comm = rounder_i inLinks ((cmult, cmult.ci) ~> (rounder_i, rounder_i.din)) outLinks ((rounder_i, rounder_i.dout) ~> douti)

      :=(bf2i_comm, bf2i_sreg_r_comm, bf2i_sreg_i_comm, bf2ii_comm, bf2ii_sreg_r_comm, bf2ii_sreg_i_comm, cmult_comm, rounder_r_comm, rounder_i_comm)
    }

    override val name: String = inst + "_stage"
    override val width: Int = data_width

    override def dfg: Graph[DfgNode, DfgEdge] = model(Seq(cmb))
  }

  /* val stage = Stage("stagetInst", 16, 2, 4)
  val compHdl = new VhdlComponentCodeGen(stage).getHdlFile()
  VhdlOutput.print(compHdl) */


  case class LastOddStage(inst: String, data_width: Int, b1_depth: Int) extends Component {

    //inputs
    val (en, s, dinr, dini) = (Streamer("en", 1), Streamer("s", 1), Streamer("dinr", data_width), Streamer("dini", data_width))
    // outputs
    val (doutr, douti) = (Streamer("doutr", data_width + 1), Streamer("douti", data_width + 1))

    override val iopaths = List((dinr, doutr))

    val cmb = Combinational {
      // BF2I
      val bf2i = BF2I(inst + "_bf2i", data_width)
      val bf2i_comm = bf2i inLinks(s ~> (bf2i, bf2i.s), dinr ~> (bf2i, bf2i.xpr), dini ~> (bf2i, bf2i.xpi)) outLinks((bf2i, bf2i.znr) ~> doutr, (bf2i, bf2i.zni) ~> douti)

      // BF2II shift registers
      val bf2i_sreg_r = Delay(inst + "_bf2i_sreg_r", data_width + 1, b1_depth)
      val bf2i_sreg_r_comm = bf2i_sreg_r inLinks(en ~> (bf2i_sreg_r.en), (bf2i, bf2i.zfr) ~> (bf2i_sreg_r, bf2i_sreg_r.din)) outLinks ((bf2i_sreg_r, bf2i_sreg_r.dout) ~> (bf2i, bf2i.xfr))
      val bf2i_sreg_i = Delay(inst + "_bf2i_sreg_i", data_width + 1, b1_depth)
      val bf2i_sreg_i_comm = bf2i_sreg_i inLinks(en ~> (bf2i_sreg_i.en), (bf2i, bf2i.zfi) ~> (bf2i_sreg_i, bf2i_sreg_i.din)) outLinks ((bf2i_sreg_i, bf2i_sreg_i.dout) ~> (bf2i, bf2i.xfi))

      :=(bf2i_comm, bf2i_sreg_r_comm, bf2i_sreg_i_comm)
    }

    override val name: String = inst + "_oddstage"
    override val width: Int = data_width

    override def dfg: Graph[DfgNode, DfgEdge] = model(Seq(cmb))
  }

  case class LastEvenStage(inst: String, data_width: Int, b1_depth: Int, b2_depth: Int) extends Component {

    //inputs
    val (en, s1, s2, dinr, dini) = (Streamer("en", 1), Streamer("s1", 1), Streamer("s2", 1), Streamer("dinr", data_width), Streamer("dini", data_width))
    // outputs
    val (doutr, douti) = (Streamer("doutr", data_width + 2), Streamer("douti", data_width + 2))

    override val iopaths = List((dinr, doutr))

    val cmb = Combinational {
      // BF2I
      val bf2i = BF2I(inst + "_bf2i", data_width)
      val bf2i_comm = bf2i inLinks(s1 ~> (bf2i, bf2i.s), dinr ~> (bf2i, bf2i.xpr), dini ~> (bf2i, bf2i.xpi))

      // BF2I shift registers
      val bf2i_sreg_r = Delay(inst + "_bf2i_sreg_r", data_width + 1, b1_depth)
      val bf2i_sreg_r_comm = bf2i_sreg_r inLinks(en ~> (bf2i_sreg_r.en), (bf2i, bf2i.zfr) ~> (bf2i_sreg_r, bf2i_sreg_r.din)) outLinks ((bf2i_sreg_r, bf2i_sreg_r.dout) ~> (bf2i, bf2i.xfr))
      val bf2i_sreg_i = Delay(inst + "_bf2i_sreg_i", data_width + 1, b1_depth)
      val bf2i_sreg_i_comm = bf2i_sreg_i inLinks(en ~> (bf2i_sreg_i.en), (bf2i, bf2i.zfi) ~> (bf2i_sreg_i, bf2i_sreg_i.din)) outLinks ((bf2i_sreg_i, bf2i_sreg_i.dout) ~> (bf2i, bf2i.xfi))

      // BF2II
      val bf2ii = BF2II(inst + "_bf2iiInst", data_width + 1)
      val bf2ii_comm = bf2ii inLinks(s2 ~> (bf2ii, bf2ii.s), s1 ~> (bf2ii, bf2ii.t), (bf2i, bf2i.znr) ~> (bf2ii, bf2ii.xpr), (bf2i, bf2i.zni) ~> (bf2ii, bf2ii.xpi)) outLinks ((bf2ii, bf2ii.znr) ~> doutr, (bf2ii, bf2ii.zni) ~> douti)

      // BF2II shift registers
      val bf2ii_sreg_r = Delay(inst + "_bf2ii_sreg_r", data_width + 2, b2_depth)
      val bf2ii_sreg_r_comm = bf2ii_sreg_r inLinks(en ~> (bf2ii_sreg_r.en), (bf2ii, bf2ii.zfr) ~> (bf2ii_sreg_r, bf2ii_sreg_r.din)) outLinks ((bf2ii_sreg_r, bf2ii_sreg_r.dout) ~> (bf2ii, bf2ii.xfr))
      val bf2ii_sreg_i = Delay(inst + "_bf2ii_sreg_i", data_width + 2, b2_depth)
      val bf2ii_sreg_i_comm = bf2ii_sreg_i inLinks(en ~> (bf2ii_sreg_i.en), (bf2ii, bf2ii.zfi) ~> (bf2ii_sreg_i, bf2ii_sreg_i.din)) outLinks ((bf2ii_sreg_i, bf2ii_sreg_i.dout) ~> (bf2ii, bf2ii.xfi))

      :=(bf2i_comm, bf2i_sreg_r_comm, bf2i_sreg_i_comm, bf2ii_comm, bf2ii_sreg_r_comm, bf2ii_sreg_i_comm)
    }

    override val name: String = inst + "_evenstage"
    override val width: Int = data_width

    override def dfg: Graph[DfgNode, DfgEdge] = model(Seq(cmb))
  }

  /* val stage = LastOddStage("lastOddStageInst", 16, 1)
  val compHdl = new VhdlComponentCodeGen(stage).getHdlFile()
  VhdlOutput.print(compHdl) */

  case class FFT_N8(inst: String, w: Int) extends Module {
    //inputs
    val (en, xnr, xni) = (Streamer("en", 1), Streamer("xnr", w), Streamer("xni", w))
    // outputs
    val (xkr, xki) = (Streamer("xkr", w + 3), Streamer("xki", w + 3))

    override val iopaths = List((xnr, xkr))

    //  val cmb = Combinational {
    // counter - fft controller
    val ctrl = Counter("ctrl", 3)
    val ctrl_comm = ctrl inLinks (en ~> (ctrl, ctrl.en))

    // Twiddle Factor ROM
    val rom_r = Rom("twiddle_r", 16, Seq(16384, 0, 16384, 11585, 16384, -11585, 16384, 16384))
    val rom_r_comm = rom_r inLinks ((ctrl, ctrl.dout) ~> (rom_r, rom_r.addr))
    val rom_i = Rom("twiddle_i", 16, Seq(0, -16384, 0, -11585, 0, -11585, 0, 0))
    val rom_i_comm = rom_i inLinks ((ctrl, ctrl.dout) ~> (rom_i, rom_i.addr))

    // stages
    val fs = Stage("first", w, 4, 2)
    val fs_comm = fs inLinks(en ~> (fs, fs.en), (ctrl, ctrl.dout, 2) ~> (fs, fs.s1), (ctrl, ctrl.dout, 1) ~> (fs, fs.s2), (rom_r, rom_r.dout) ~> (fs, fs.tfr), (rom_i, rom_i.dout) ~> (fs, fs.tfi), xnr ~> (fs, fs.dinr), xni ~> (fs, fs.dini))

    // last stage
    val ls = LastOddStage("last", w + 2, 1)
    val ls_comm = ls inLinks(en ~> (ls, ls.en), (ctrl, ctrl.dout, 0) ~> (ls, ls.s), (fs, fs.doutr) ~> (ls, ls.dinr), (fs, fs.douti) ~> (ls, ls.dini)) outLinks((ls, ls.doutr) ~> xkr, (ls, ls.douti) ~> xki)

    //   :=(ctrl_comm, rom_r_comm, rom_i_comm, fs_comm, ls_comm)
    //}

    override val name: String = "fft_n8"
    override val width: Int = w
    override val dfg = model(Seq(ctrl_comm, rom_r_comm, rom_i_comm, fs_comm, ls_comm))
  }

  /* val fft8 = FFT_N8("fft8", 16)
   val compHdl = new VhdlComponentCodeGen(fft8).getHdlFile()
   VhdlOutput.print(compHdl) */

  case class FFT64(inst: String, w: Int) extends Component {
    //inputs
    val (en, xnr, xni) = (Streamer("en", 1), Streamer("xnr", w), Streamer("xni", w))
    // outputs
    val (xkr, xki) = (Streamer("xkr", w + 6), Streamer("xki", w + 6))

    override val iopaths = List((xnr, xkr))

    val cmb = Combinational {
      // counter - fft controller
      val ctrl = Counter("ctrl", 6)
      val ctrl_comm = ctrl inLinks (en ~> (ctrl, ctrl.en))

      // Twiddle Factor ROM 1
      val rom1_r = Rom("stage1_twiddle_r", 16, Seq(16384, 16069, 15137, 13623, 11585, 9102, 6270, 3196, 0, -3196, -6270, -9102, -11585, -13623, -15137, -16069, 16384, 16305, 16069, 15679, 15137, 14449, 13623, 12665, 11585, 10394, 9102, 7723, 6270, 4756, 3196, 1606, 16384, 15679, 13623, 10394, 6270, 1606, -3196, -7723, -11585, -14449, -16069, -16305, -15137, -12665, -9102, -4756, 16384, 16384, 16384, 16384, 16384, 16384, 16384, 16384, 16384, 16384, 16384, 16384, 16384, 16384, 16384, 16384))
      val rom1_r_comm = rom1_r inLinks ((ctrl, ctrl.dout) ~> (rom1_r, rom1_r.addr))
      val rom1_i = Rom("stage1_twiddle_i", 16, Seq(0, -3196, -6270, -9102, -11585, -13623, -15137, -16069, -16384, -16069, -15137, -13623, -11585, -9102, -6270, -3196, 0, -1606, -3196, -4756, -6270, -7723, -9102, -10394, -11585, -12665, -13623, -14449, -15137, -15679, -16069, -16305, 0, -4756, -9102, -12665, -15137, -16305, -16069, -14449, -11585, -7723, -3196, 1606, 6270, 10394, 13623, 15679, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
      val rom1_i_comm = rom1_i inLinks ((ctrl, ctrl.dout) ~> (rom1_i, rom1_i.addr))

      // Twiddle Factor ROM 1
      val rom2_r = Rom("stage2_twiddle_r", 16, Seq(16384, 11585, 0, -11585, 16384, 15137, 11585, 6270, 16384, 6270, -11585, -15137, 16384, 16384, 16384, 16384))
      val rom2_r_comm = rom2_r inLinks ((ctrl, ctrl.dout, 3, 0) ~> (rom2_r, rom2_r.addr))
      val rom2_i = Rom("stage2_twiddle_i", 16, Seq(0, -11585, -16384, -11585, 0, -6270, -11585, -15137, 0, -15137, -11585, 6270, 0, 0, 0, 0))
      val rom2_i_comm = rom2_i inLinks ((ctrl, ctrl.dout, 3, 0) ~> (rom2_i, rom2_i.addr))

      // -- stages
      // stage 1
      val fs1 = Stage("first", w, 32, 16)
      val fs1_comm = fs1 inLinks(en ~> (fs1, fs1.en), (ctrl, ctrl.dout, 5) ~> (fs1, fs1.s1), (ctrl, ctrl.dout, 4) ~> (fs1, fs1.s2), (rom1_r, rom1_r.dout) ~> (fs1, fs1.tfr), (rom1_i, rom1_i.dout) ~> (fs1, fs1.tfi), xnr ~> (fs1, fs1.dinr), xni ~> (fs1, fs1.dini))

      // stage 1
      val fs2 = Stage("sec", w + 2, 8, 4)
      val fs2_comm = fs2 inLinks(en ~> (fs2, fs2.en), (ctrl, ctrl.dout, 3) ~> (fs2, fs2.s1), (ctrl, ctrl.dout, 2) ~> (fs2, fs2.s2), (rom2_r, rom2_r.dout) ~> (fs2, fs2.tfr), (rom2_i, rom2_i.dout) ~> (fs2, fs2.tfi), (fs1, fs1.doutr) ~> (fs2, fs2.dinr), (fs1, fs1.douti) ~> (fs2, fs2.dini))

      // last stage
      val ls = LastEvenStage("last", w + 4, 2, 1)
      val ls_comm = ls inLinks(en ~> (ls, ls.en), (ctrl, ctrl.dout, 1) ~> (ls, ls.s1), (ctrl, ctrl.dout, 0) ~> (ls, ls.s2), (fs2, fs2.doutr) ~> (ls, ls.dinr), (fs2, fs2.douti) ~> (ls, ls.dini)) outLinks((ls, ls.doutr) ~> xkr, (ls, ls.douti) ~> xki)
      :=(ctrl_comm, rom1_r_comm, rom1_i_comm, rom2_r_comm, rom2_i_comm, fs1_comm, fs2_comm, ls_comm)
    }

    override val name: String = "fft_n64"
    override val width: Int = w
    override val dfg = model(Seq(cmb))
  }

  case class FFT_N64(inst: String, w: Int) extends Module {
    //inputs
    val (en, xnr, xni) = (Streamer("en", 1), Streamer("xnr", w), Streamer("xni", w))
    // outputs
    val (xkr, xki) = (Streamer("xkr", w + 6), Streamer("xki", w + 6))

    override val iopaths = List((xnr, xkr))

    val fft = FFT64("fft64_Inst", w)
    val fft_comm = fft inLinks(en ~> (fft, fft.en), xnr ~> (fft, fft.xnr), xni ~> (fft, fft.xni)) outLinks((fft, fft.xkr) ~> xkr, (fft, fft.xki) ~> xki)

    override val name: String = fft.name
    override val width: Int = w
    override val dfg = fft.dfg
  }

  case class IFFT_N64(inst: String, w: Int) extends Module {
    //inputs
    val (en, xnr, xni) = (Streamer("en", 1), Streamer("xnr", w), Streamer("xni", w))
    // outputs
    val (xkr, xki) = (Streamer("xkr", w + 6), Streamer("xki", w + 6))

    override val iopaths = List((xnr, xkr))

    val fft = FFT64("fft64_Inst", w)
    val fft_comm = fft inLinks(en ~> (fft, fft.en), xnr ~> (fft, fft.xni), xni ~> (fft, fft.xnr)) outLinks((fft, fft.xki) ~> xkr, (fft, fft.xkr) ~> xki)

    override val name: String = "ifft_n64"
    override val width: Int = w
    override val dfg = model(Seq(fft_comm))
  }

  case class FFT2048(inst: String, w: Int) extends Component {
    //inputs
    val (en, xnr, xni) = (Streamer("en", 1), Streamer("xnr", w), Streamer("xni", w))
    // outputs
    val (xkr, xki) = (Streamer("xkr", w + 11), Streamer("xki", w + 11))

    override val iopaths = List((xnr, xkr))

    val cmb = Combinational {
      // counter - fft controller
      val ctrl = Counter("ctrl", 11)
      val ctrl_comm = ctrl inLinks (en ~> (ctrl, ctrl.en))

      // Twiddle Factor ROM 1
      val rom1_r = Rom("stage1_twiddle_r", 16, Seq(16384,16384,16383,16381,16379,16376,16373,16369,16364,16359,16353,16347,16340,16332,16324,16315,16305,16295,16284,16273,16261,16248,16235,16221,16207,16192,16176,16160,16143,16125,16107,16088,16069,16049,16029,16008,15986,15964,15941,15917,15893,15868,15843,15817,15791,15763,15736,15707,15679,15649,15619,15588,15557,15525,15493,15460,15426,15392,15357,15322,15286,15250,15213,15175,15137,15098,15059,15019,14978,14937,14896,14854,14811,14768,14724,14680,14635,14589,14543,14497,14449,14402,14354,14305,14256,14206,14155,14104,14053,14001,13949,13896,13842,13788,13733,13678,13623,13567,13510,13453,13395,13337,13279,13219,13160,13100,13039,12978,12916,12854,12792,12729,12665,12601,12537,12472,12406,12340,12274,12207,12140,12072,12004,11935,11866,11797,11727,11656,11585,11514,11442,11370,11297,11224,11151,11077,11003,10928,10853,10778,10702,10625,10549,10471,10394,10316,10238,10159,10080,10001,9921,9841,9760,9679,9598,9516,9434,9352,9269,9186,9102,9019,8935,8850,8765,8680,8595,8509,8423,8337,8250,8163,8076,7988,7900,7812,7723,7635,7545,7456,7366,7276,7186,7096,7005,6914,6823,6731,6639,6547,6455,6363,6270,6177,6084,5990,5897,5803,5708,5614,5520,5425,5330,5235,5139,5044,4948,4852,4756,4660,4563,4467,4370,4273,4176,4078,3981,3883,3786,3688,3590,3492,3393,3295,3196,3098,2999,2900,2801,2702,2603,2503,2404,2305,2205,2105,2006,1906,1806,1706,1606,1506,1406,1306,1205,1105,1005,904,804,704,603,503,402,302,201,101,0,-101,-201,-302,-402,-503,-603,-704,-804,-904,-1005,-1105,-1205,-1306,-1406,-1506,-1606,-1706,-1806,-1906,-2006,-2105,-2205,-2305,-2404,-2503,-2603,-2702,-2801,-2900,-2999,-3098,-3196,-3295,-3393,-3492,-3590,-3688,-3786,-3883,-3981,-4078,-4176,-4273,-4370,-4467,-4563,-4660,-4756,-4852,-4948,-5044,-5139,-5235,-5330,-5425,-5520,-5614,-5708,-5803,-5897,-5990,-6084,-6177,-6270,-6363,-6455,-6547,-6639,-6731,-6823,-6914,-7005,-7096,-7186,-7276,-7366,-7456,-7545,-7635,-7723,-7812,-7900,-7988,-8076,-8163,-8250,-8337,-8423,-8509,-8595,-8680,-8765,-8850,-8935,-9019,-9102,-9186,-9269,-9352,-9434,-9516,-9598,-9679,-9760,-9841,-9921,-10001,-10080,-10159,-10238,-10316,-10394,-10471,-10549,-10625,-10702,-10778,-10853,-10928,-11003,-11077,-11151,-11224,-11297,-11370,-11442,-11514,-11585,-11656,-11727,-11797,-11866,-11935,-12004,-12072,-12140,-12207,-12274,-12340,-12406,-12472,-12537,-12601,-12665,-12729,-12792,-12854,-12916,-12978,-13039,-13100,-13160,-13219,-13279,-13337,-13395,-13453,-13510,-13567,-13623,-13678,-13733,-13788,-13842,-13896,-13949,-14001,-14053,-14104,-14155,-14206,-14256,-14305,-14354,-14402,-14449,-14497,-14543,-14589,-14635,-14680,-14724,-14768,-14811,-14854,-14896,-14937,-14978,-15019,-15059,-15098,-15137,-15175,-15213,-15250,-15286,-15322,-15357,-15392,-15426,-15460,-15493,-15525,-15557,-15588,-15619,-15649,-15679,-15707,-15736,-15763,-15791,-15817,-15843,-15868,-15893,-15917,-15941,-15964,-15986,-16008,-16029,-16049,-16069,-16088,-16107,-16125,-16143,-16160,-16176,-16192,-16207,-16221,-16235,-16248,-16261,-16273,-16284,-16295,-16305,-16315,-16324,-16332,-16340,-16347,-16353,-16359,-16364,-16369,-16373,-16376,-16379,-16381,-16383,-16384,16384,16384,16384,16383,16383,16382,16381,16380,16379,16378,16376,16375,16373,16371,16369,16367,16364,16362,16359,16356,16353,16350,16347,16343,16340,16336,16332,16328,16324,16319,16315,16310,16305,16300,16295,16290,16284,16279,16273,16267,16261,16255,16248,16242,16235,16228,16221,16214,16207,16199,16192,16184,16176,16168,16160,16151,16143,16134,16125,16116,16107,16098,16088,16079,16069,16059,16049,16039,16029,16018,16008,15997,15986,15975,15964,15952,15941,15929,15917,15905,15893,15881,15868,15856,15843,15830,15817,15804,15791,15777,15763,15750,15736,15722,15707,15693,15679,15664,15649,15634,15619,15604,15588,15573,15557,15541,15525,15509,15493,15476,15460,15443,15426,15409,15392,15375,15357,15340,15322,15304,15286,15268,15250,15231,15213,15194,15175,15156,15137,15118,15098,15078,15059,15039,15019,14999,14978,14958,14937,14917,14896,14875,14854,14832,14811,14789,14768,14746,14724,14702,14680,14657,14635,14612,14589,14566,14543,14520,14497,14473,14449,14426,14402,14378,14354,14329,14305,14280,14256,14231,14206,14181,14155,14130,14104,14079,14053,14027,14001,13975,13949,13922,13896,13869,13842,13815,13788,13761,13733,13706,13678,13651,13623,13595,13567,13538,13510,13482,13453,13424,13395,13366,13337,13308,13279,13249,13219,13190,13160,13130,13100,13069,13039,13008,12978,12947,12916,12885,12854,12823,12792,12760,12729,12697,12665,12633,12601,12569,12537,12504,12472,12439,12406,12373,12340,12307,12274,12240,12207,12173,12140,12106,12072,12038,12004,11970,11935,11901,11866,11831,11797,11762,11727,11691,11656,11621,11585,11550,11514,11478,11442,11406,11370,11334,11297,11261,11224,11188,11151,11114,11077,11040,11003,10966,10928,10891,10853,10815,10778,10740,10702,10663,10625,10587,10549,10510,10471,10433,10394,10355,10316,10277,10238,10198,10159,10120,10080,10040,10001,9961,9921,9881,9841,9800,9760,9720,9679,9638,9598,9557,9516,9475,9434,9393,9352,9310,9269,9227,9186,9144,9102,9061,9019,8977,8935,8892,8850,8808,8765,8723,8680,8638,8595,8552,8509,8466,8423,8380,8337,8293,8250,8207,8163,8119,8076,8032,7988,7944,7900,7856,7812,7768,7723,7679,7635,7590,7545,7501,7456,7411,7366,7321,7276,7231,7186,7141,7096,7050,7005,6960,6914,6868,6823,6777,6731,6685,6639,6593,6547,6501,6455,6409,6363,6316,6270,6223,6177,6130,6084,6037,5990,5943,5897,5850,5803,5756,5708,5661,5614,5567,5520,5472,5425,5377,5330,5282,5235,5187,5139,5092,5044,4996,4948,4900,4852,4804,4756,4708,4660,4612,4563,4515,4467,4418,4370,4321,4273,4224,4176,4127,4078,4030,3981,3932,3883,3835,3786,3737,3688,3639,3590,3541,3492,3442,3393,3344,3295,3246,3196,3147,3098,3048,2999,2949,2900,2851,2801,2752,2702,2652,2603,2553,2503,2454,2404,2354,2305,2255,2205,2155,2105,2055,2006,1956,1906,1856,1806,1756,1706,1656,1606,1556,1506,1456,1406,1356,1306,1255,1205,1155,1105,1055,1005,955,904,854,804,754,704,653,603,553,503,452,402,352,302,251,201,151,101,50,16384,16383,16381,16378,16373,16367,16359,16350,16340,16328,16315,16300,16284,16267,16248,16228,16207,16184,16160,16134,16107,16079,16049,16018,15986,15952,15917,15881,15843,15804,15763,15722,15679,15634,15588,15541,15493,15443,15392,15340,15286,15231,15175,15118,15059,14999,14937,14875,14811,14746,14680,14612,14543,14473,14402,14329,14256,14181,14104,14027,13949,13869,13788,13706,13623,13538,13453,13366,13279,13190,13100,13008,12916,12823,12729,12633,12537,12439,12340,12240,12140,12038,11935,11831,11727,11621,11514,11406,11297,11188,11077,10966,10853,10740,10625,10510,10394,10277,10159,10040,9921,9800,9679,9557,9434,9310,9186,9061,8935,8808,8680,8552,8423,8293,8163,8032,7900,7768,7635,7501,7366,7231,7096,6960,6823,6685,6547,6409,6270,6130,5990,5850,5708,5567,5425,5282,5139,4996,4852,4708,4563,4418,4273,4127,3981,3835,3688,3541,3393,3246,3098,2949,2801,2652,2503,2354,2205,2055,1906,1756,1606,1456,1306,1155,1005,854,704,553,402,251,101,-50,-201,-352,-503,-653,-804,-955,-1105,-1255,-1406,-1556,-1706,-1856,-2006,-2155,-2305,-2454,-2603,-2752,-2900,-3048,-3196,-3344,-3492,-3639,-3786,-3932,-4078,-4224,-4370,-4515,-4660,-4804,-4948,-5092,-5235,-5377,-5520,-5661,-5803,-5943,-6084,-6223,-6363,-6501,-6639,-6777,-6914,-7050,-7186,-7321,-7456,-7590,-7723,-7856,-7988,-8119,-8250,-8380,-8509,-8638,-8765,-8892,-9019,-9144,-9269,-9393,-9516,-9638,-9760,-9881,-10001,-10120,-10238,-10355,-10471,-10587,-10702,-10815,-10928,-11040,-11151,-11261,-11370,-11478,-11585,-11691,-11797,-11901,-12004,-12106,-12207,-12307,-12406,-12504,-12601,-12697,-12792,-12885,-12978,-13069,-13160,-13249,-13337,-13424,-13510,-13595,-13678,-13761,-13842,-13922,-14001,-14079,-14155,-14231,-14305,-14378,-14449,-14520,-14589,-14657,-14724,-14789,-14854,-14917,-14978,-15039,-15098,-15156,-15213,-15268,-15322,-15375,-15426,-15476,-15525,-15573,-15619,-15664,-15707,-15750,-15791,-15830,-15868,-15905,-15941,-15975,-16008,-16039,-16069,-16098,-16125,-16151,-16176,-16199,-16221,-16242,-16261,-16279,-16295,-16310,-16324,-16336,-16347,-16356,-16364,-16371,-16376,-16380,-16383,-16384,-16384,-16382,-16379,-16375,-16369,-16362,-16353,-16343,-16332,-16319,-16305,-16290,-16273,-16255,-16235,-16214,-16192,-16168,-16143,-16116,-16088,-16059,-16029,-15997,-15964,-15929,-15893,-15856,-15817,-15777,-15736,-15693,-15649,-15604,-15557,-15509,-15460,-15409,-15357,-15304,-15250,-15194,-15137,-15078,-15019,-14958,-14896,-14832,-14768,-14702,-14635,-14566,-14497,-14426,-14354,-14280,-14206,-14130,-14053,-13975,-13896,-13815,-13733,-13651,-13567,-13482,-13395,-13308,-13219,-13130,-13039,-12947,-12854,-12760,-12665,-12569,-12472,-12373,-12274,-12173,-12072,-11970,-11866,-11762,-11656,-11550,-11442,-11334,-11224,-11114,-11003,-10891,-10778,-10663,-10549,-10433,-10316,-10198,-10080,-9961,-9841,-9720,-9598,-9475,-9352,-9227,-9102,-8977,-8850,-8723,-8595,-8466,-8337,-8207,-8076,-7944,-7812,-7679,-7545,-7411,-7276,-7141,-7005,-6868,-6731,-6593,-6455,-6316,-6177,-6037,-5897,-5756,-5614,-5472,-5330,-5187,-5044,-4900,-4756,-4612,-4467,-4321,-4176,-4030,-3883,-3737,-3590,-3442,-3295,-3147,-2999,-2851,-2702,-2553,-2404,-2255,-2105,-1956,-1806,-1656,-1506,-1356,-1205,-1055,-904,-754,-603,-452,-302,-151,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384))
      val rom1_r_comm = rom1_r inLinks ((ctrl, ctrl.dout) ~> (rom1_r, rom1_r.addr))
      val rom1_i = Rom("stage1_twiddle_i", 16, Seq(0,-101,-201,-302,-402,-503,-603,-704,-804,-904,-1005,-1105,-1205,-1306,-1406,-1506,-1606,-1706,-1806,-1906,-2006,-2105,-2205,-2305,-2404,-2503,-2603,-2702,-2801,-2900,-2999,-3098,-3196,-3295,-3393,-3492,-3590,-3688,-3786,-3883,-3981,-4078,-4176,-4273,-4370,-4467,-4563,-4660,-4756,-4852,-4948,-5044,-5139,-5235,-5330,-5425,-5520,-5614,-5708,-5803,-5897,-5990,-6084,-6177,-6270,-6363,-6455,-6547,-6639,-6731,-6823,-6914,-7005,-7096,-7186,-7276,-7366,-7456,-7545,-7635,-7723,-7812,-7900,-7988,-8076,-8163,-8250,-8337,-8423,-8509,-8595,-8680,-8765,-8850,-8935,-9019,-9102,-9186,-9269,-9352,-9434,-9516,-9598,-9679,-9760,-9841,-9921,-10001,-10080,-10159,-10238,-10316,-10394,-10471,-10549,-10625,-10702,-10778,-10853,-10928,-11003,-11077,-11151,-11224,-11297,-11370,-11442,-11514,-11585,-11656,-11727,-11797,-11866,-11935,-12004,-12072,-12140,-12207,-12274,-12340,-12406,-12472,-12537,-12601,-12665,-12729,-12792,-12854,-12916,-12978,-13039,-13100,-13160,-13219,-13279,-13337,-13395,-13453,-13510,-13567,-13623,-13678,-13733,-13788,-13842,-13896,-13949,-14001,-14053,-14104,-14155,-14206,-14256,-14305,-14354,-14402,-14449,-14497,-14543,-14589,-14635,-14680,-14724,-14768,-14811,-14854,-14896,-14937,-14978,-15019,-15059,-15098,-15137,-15175,-15213,-15250,-15286,-15322,-15357,-15392,-15426,-15460,-15493,-15525,-15557,-15588,-15619,-15649,-15679,-15707,-15736,-15763,-15791,-15817,-15843,-15868,-15893,-15917,-15941,-15964,-15986,-16008,-16029,-16049,-16069,-16088,-16107,-16125,-16143,-16160,-16176,-16192,-16207,-16221,-16235,-16248,-16261,-16273,-16284,-16295,-16305,-16315,-16324,-16332,-16340,-16347,-16353,-16359,-16364,-16369,-16373,-16376,-16379,-16381,-16383,-16384,-16384,-16384,-16383,-16381,-16379,-16376,-16373,-16369,-16364,-16359,-16353,-16347,-16340,-16332,-16324,-16315,-16305,-16295,-16284,-16273,-16261,-16248,-16235,-16221,-16207,-16192,-16176,-16160,-16143,-16125,-16107,-16088,-16069,-16049,-16029,-16008,-15986,-15964,-15941,-15917,-15893,-15868,-15843,-15817,-15791,-15763,-15736,-15707,-15679,-15649,-15619,-15588,-15557,-15525,-15493,-15460,-15426,-15392,-15357,-15322,-15286,-15250,-15213,-15175,-15137,-15098,-15059,-15019,-14978,-14937,-14896,-14854,-14811,-14768,-14724,-14680,-14635,-14589,-14543,-14497,-14449,-14402,-14354,-14305,-14256,-14206,-14155,-14104,-14053,-14001,-13949,-13896,-13842,-13788,-13733,-13678,-13623,-13567,-13510,-13453,-13395,-13337,-13279,-13219,-13160,-13100,-13039,-12978,-12916,-12854,-12792,-12729,-12665,-12601,-12537,-12472,-12406,-12340,-12274,-12207,-12140,-12072,-12004,-11935,-11866,-11797,-11727,-11656,-11585,-11514,-11442,-11370,-11297,-11224,-11151,-11077,-11003,-10928,-10853,-10778,-10702,-10625,-10549,-10471,-10394,-10316,-10238,-10159,-10080,-10001,-9921,-9841,-9760,-9679,-9598,-9516,-9434,-9352,-9269,-9186,-9102,-9019,-8935,-8850,-8765,-8680,-8595,-8509,-8423,-8337,-8250,-8163,-8076,-7988,-7900,-7812,-7723,-7635,-7545,-7456,-7366,-7276,-7186,-7096,-7005,-6914,-6823,-6731,-6639,-6547,-6455,-6363,-6270,-6177,-6084,-5990,-5897,-5803,-5708,-5614,-5520,-5425,-5330,-5235,-5139,-5044,-4948,-4852,-4756,-4660,-4563,-4467,-4370,-4273,-4176,-4078,-3981,-3883,-3786,-3688,-3590,-3492,-3393,-3295,-3196,-3098,-2999,-2900,-2801,-2702,-2603,-2503,-2404,-2305,-2205,-2105,-2006,-1906,-1806,-1706,-1606,-1506,-1406,-1306,-1205,-1105,-1005,-904,-804,-704,-603,-503,-402,-302,-201,-101,0,-50,-101,-151,-201,-251,-302,-352,-402,-452,-503,-553,-603,-653,-704,-754,-804,-854,-904,-955,-1005,-1055,-1105,-1155,-1205,-1255,-1306,-1356,-1406,-1456,-1506,-1556,-1606,-1656,-1706,-1756,-1806,-1856,-1906,-1956,-2006,-2055,-2105,-2155,-2205,-2255,-2305,-2354,-2404,-2454,-2503,-2553,-2603,-2652,-2702,-2752,-2801,-2851,-2900,-2949,-2999,-3048,-3098,-3147,-3196,-3246,-3295,-3344,-3393,-3442,-3492,-3541,-3590,-3639,-3688,-3737,-3786,-3835,-3883,-3932,-3981,-4030,-4078,-4127,-4176,-4224,-4273,-4321,-4370,-4418,-4467,-4515,-4563,-4612,-4660,-4708,-4756,-4804,-4852,-4900,-4948,-4996,-5044,-5092,-5139,-5187,-5235,-5282,-5330,-5377,-5425,-5472,-5520,-5567,-5614,-5661,-5708,-5756,-5803,-5850,-5897,-5943,-5990,-6037,-6084,-6130,-6177,-6223,-6270,-6316,-6363,-6409,-6455,-6501,-6547,-6593,-6639,-6685,-6731,-6777,-6823,-6868,-6914,-6960,-7005,-7050,-7096,-7141,-7186,-7231,-7276,-7321,-7366,-7411,-7456,-7501,-7545,-7590,-7635,-7679,-7723,-7768,-7812,-7856,-7900,-7944,-7988,-8032,-8076,-8119,-8163,-8207,-8250,-8293,-8337,-8380,-8423,-8466,-8509,-8552,-8595,-8638,-8680,-8723,-8765,-8808,-8850,-8892,-8935,-8977,-9019,-9061,-9102,-9144,-9186,-9227,-9269,-9310,-9352,-9393,-9434,-9475,-9516,-9557,-9598,-9638,-9679,-9720,-9760,-9800,-9841,-9881,-9921,-9961,-10001,-10040,-10080,-10120,-10159,-10198,-10238,-10277,-10316,-10355,-10394,-10433,-10471,-10510,-10549,-10587,-10625,-10663,-10702,-10740,-10778,-10815,-10853,-10891,-10928,-10966,-11003,-11040,-11077,-11114,-11151,-11188,-11224,-11261,-11297,-11334,-11370,-11406,-11442,-11478,-11514,-11550,-11585,-11621,-11656,-11691,-11727,-11762,-11797,-11831,-11866,-11901,-11935,-11970,-12004,-12038,-12072,-12106,-12140,-12173,-12207,-12240,-12274,-12307,-12340,-12373,-12406,-12439,-12472,-12504,-12537,-12569,-12601,-12633,-12665,-12697,-12729,-12760,-12792,-12823,-12854,-12885,-12916,-12947,-12978,-13008,-13039,-13069,-13100,-13130,-13160,-13190,-13219,-13249,-13279,-13308,-13337,-13366,-13395,-13424,-13453,-13482,-13510,-13538,-13567,-13595,-13623,-13651,-13678,-13706,-13733,-13761,-13788,-13815,-13842,-13869,-13896,-13922,-13949,-13975,-14001,-14027,-14053,-14079,-14104,-14130,-14155,-14181,-14206,-14231,-14256,-14280,-14305,-14329,-14354,-14378,-14402,-14426,-14449,-14473,-14497,-14520,-14543,-14566,-14589,-14612,-14635,-14657,-14680,-14702,-14724,-14746,-14768,-14789,-14811,-14832,-14854,-14875,-14896,-14917,-14937,-14958,-14978,-14999,-15019,-15039,-15059,-15078,-15098,-15118,-15137,-15156,-15175,-15194,-15213,-15231,-15250,-15268,-15286,-15304,-15322,-15340,-15357,-15375,-15392,-15409,-15426,-15443,-15460,-15476,-15493,-15509,-15525,-15541,-15557,-15573,-15588,-15604,-15619,-15634,-15649,-15664,-15679,-15693,-15707,-15722,-15736,-15750,-15763,-15777,-15791,-15804,-15817,-15830,-15843,-15856,-15868,-15881,-15893,-15905,-15917,-15929,-15941,-15952,-15964,-15975,-15986,-15997,-16008,-16018,-16029,-16039,-16049,-16059,-16069,-16079,-16088,-16098,-16107,-16116,-16125,-16134,-16143,-16151,-16160,-16168,-16176,-16184,-16192,-16199,-16207,-16214,-16221,-16228,-16235,-16242,-16248,-16255,-16261,-16267,-16273,-16279,-16284,-16290,-16295,-16300,-16305,-16310,-16315,-16319,-16324,-16328,-16332,-16336,-16340,-16343,-16347,-16350,-16353,-16356,-16359,-16362,-16364,-16367,-16369,-16371,-16373,-16375,-16376,-16378,-16379,-16380,-16381,-16382,-16383,-16383,-16384,-16384,0,-151,-302,-452,-603,-754,-904,-1055,-1205,-1356,-1506,-1656,-1806,-1956,-2105,-2255,-2404,-2553,-2702,-2851,-2999,-3147,-3295,-3442,-3590,-3737,-3883,-4030,-4176,-4321,-4467,-4612,-4756,-4900,-5044,-5187,-5330,-5472,-5614,-5756,-5897,-6037,-6177,-6316,-6455,-6593,-6731,-6868,-7005,-7141,-7276,-7411,-7545,-7679,-7812,-7944,-8076,-8207,-8337,-8466,-8595,-8723,-8850,-8977,-9102,-9227,-9352,-9475,-9598,-9720,-9841,-9961,-10080,-10198,-10316,-10433,-10549,-10663,-10778,-10891,-11003,-11114,-11224,-11334,-11442,-11550,-11656,-11762,-11866,-11970,-12072,-12173,-12274,-12373,-12472,-12569,-12665,-12760,-12854,-12947,-13039,-13130,-13219,-13308,-13395,-13482,-13567,-13651,-13733,-13815,-13896,-13975,-14053,-14130,-14206,-14280,-14354,-14426,-14497,-14566,-14635,-14702,-14768,-14832,-14896,-14958,-15019,-15078,-15137,-15194,-15250,-15304,-15357,-15409,-15460,-15509,-15557,-15604,-15649,-15693,-15736,-15777,-15817,-15856,-15893,-15929,-15964,-15997,-16029,-16059,-16088,-16116,-16143,-16168,-16192,-16214,-16235,-16255,-16273,-16290,-16305,-16319,-16332,-16343,-16353,-16362,-16369,-16375,-16379,-16382,-16384,-16384,-16383,-16380,-16376,-16371,-16364,-16356,-16347,-16336,-16324,-16310,-16295,-16279,-16261,-16242,-16221,-16199,-16176,-16151,-16125,-16098,-16069,-16039,-16008,-15975,-15941,-15905,-15868,-15830,-15791,-15750,-15707,-15664,-15619,-15573,-15525,-15476,-15426,-15375,-15322,-15268,-15213,-15156,-15098,-15039,-14978,-14917,-14854,-14789,-14724,-14657,-14589,-14520,-14449,-14378,-14305,-14231,-14155,-14079,-14001,-13922,-13842,-13761,-13678,-13595,-13510,-13424,-13337,-13249,-13160,-13069,-12978,-12885,-12792,-12697,-12601,-12504,-12406,-12307,-12207,-12106,-12004,-11901,-11797,-11691,-11585,-11478,-11370,-11261,-11151,-11040,-10928,-10815,-10702,-10587,-10471,-10355,-10238,-10120,-10001,-9881,-9760,-9638,-9516,-9393,-9269,-9144,-9019,-8892,-8765,-8638,-8509,-8380,-8250,-8119,-7988,-7856,-7723,-7590,-7456,-7321,-7186,-7050,-6914,-6777,-6639,-6501,-6363,-6223,-6084,-5943,-5803,-5661,-5520,-5377,-5235,-5092,-4948,-4804,-4660,-4515,-4370,-4224,-4078,-3932,-3786,-3639,-3492,-3344,-3196,-3048,-2900,-2752,-2603,-2454,-2305,-2155,-2006,-1856,-1706,-1556,-1406,-1255,-1105,-955,-804,-653,-503,-352,-201,-50,101,251,402,553,704,854,1005,1155,1306,1456,1606,1756,1906,2055,2205,2354,2503,2652,2801,2949,3098,3246,3393,3541,3688,3835,3981,4127,4273,4418,4563,4708,4852,4996,5139,5282,5425,5567,5708,5850,5990,6130,6270,6409,6547,6685,6823,6960,7096,7231,7366,7501,7635,7768,7900,8032,8163,8293,8423,8552,8680,8808,8935,9061,9186,9310,9434,9557,9679,9800,9921,10040,10159,10277,10394,10510,10625,10740,10853,10966,11077,11188,11297,11406,11514,11621,11727,11831,11935,12038,12140,12240,12340,12439,12537,12633,12729,12823,12916,13008,13100,13190,13279,13366,13453,13538,13623,13706,13788,13869,13949,14027,14104,14181,14256,14329,14402,14473,14543,14612,14680,14746,14811,14875,14937,14999,15059,15118,15175,15231,15286,15340,15392,15443,15493,15541,15588,15634,15679,15722,15763,15804,15843,15881,15917,15952,15986,16018,16049,16079,16107,16134,16160,16184,16207,16228,16248,16267,16284,16300,16315,16328,16340,16350,16359,16367,16373,16378,16381,16383,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0))
      val rom1_i_comm = rom1_i inLinks ((ctrl, ctrl.dout) ~> (rom1_i, rom1_i.addr))

      // Twiddle Factor ROM 2
      val rom2_r = Rom("stage2_twiddle_r", 16, Seq(16384,16379,16364,16340,16305,16261,16207,16143,16069,15986,15893,15791,15679,15557,15426,15286,15137,14978,14811,14635,14449,14256,14053,13842,13623,13395,13160,12916,12665,12406,12140,11866,11585,11297,11003,10702,10394,10080,9760,9434,9102,8765,8423,8076,7723,7366,7005,6639,6270,5897,5520,5139,4756,4370,3981,3590,3196,2801,2404,2006,1606,1205,804,402,0,-402,-804,-1205,-1606,-2006,-2404,-2801,-3196,-3590,-3981,-4370,-4756,-5139,-5520,-5897,-6270,-6639,-7005,-7366,-7723,-8076,-8423,-8765,-9102,-9434,-9760,-10080,-10394,-10702,-11003,-11297,-11585,-11866,-12140,-12406,-12665,-12916,-13160,-13395,-13623,-13842,-14053,-14256,-14449,-14635,-14811,-14978,-15137,-15286,-15426,-15557,-15679,-15791,-15893,-15986,-16069,-16143,-16207,-16261,-16305,-16340,-16364,-16379,16384,16383,16379,16373,16364,16353,16340,16324,16305,16284,16261,16235,16207,16176,16143,16107,16069,16029,15986,15941,15893,15843,15791,15736,15679,15619,15557,15493,15426,15357,15286,15213,15137,15059,14978,14896,14811,14724,14635,14543,14449,14354,14256,14155,14053,13949,13842,13733,13623,13510,13395,13279,13160,13039,12916,12792,12665,12537,12406,12274,12140,12004,11866,11727,11585,11442,11297,11151,11003,10853,10702,10549,10394,10238,10080,9921,9760,9598,9434,9269,9102,8935,8765,8595,8423,8250,8076,7900,7723,7545,7366,7186,7005,6823,6639,6455,6270,6084,5897,5708,5520,5330,5139,4948,4756,4563,4370,4176,3981,3786,3590,3393,3196,2999,2801,2603,2404,2205,2006,1806,1606,1406,1205,1005,804,603,402,201,16384,16373,16340,16284,16207,16107,15986,15843,15679,15493,15286,15059,14811,14543,14256,13949,13623,13279,12916,12537,12140,11727,11297,10853,10394,9921,9434,8935,8423,7900,7366,6823,6270,5708,5139,4563,3981,3393,2801,2205,1606,1005,402,-201,-804,-1406,-2006,-2603,-3196,-3786,-4370,-4948,-5520,-6084,-6639,-7186,-7723,-8250,-8765,-9269,-9760,-10238,-10702,-11151,-11585,-12004,-12406,-12792,-13160,-13510,-13842,-14155,-14449,-14724,-14978,-15213,-15426,-15619,-15791,-15941,-16069,-16176,-16261,-16324,-16364,-16383,-16379,-16353,-16305,-16235,-16143,-16029,-15893,-15736,-15557,-15357,-15137,-14896,-14635,-14354,-14053,-13733,-13395,-13039,-12665,-12274,-11866,-11442,-11003,-10549,-10080,-9598,-9102,-8595,-8076,-7545,-7005,-6455,-5897,-5330,-4756,-4176,-3590,-2999,-2404,-1806,-1205,-603,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384))
      val rom2_r_comm = rom2_r inLinks ((ctrl, ctrl.dout, 8, 0) ~> (rom2_r, rom2_r.addr))
      val rom2_i = Rom("stage2_twiddle_i", 16, Seq(0,-402,-804,-1205,-1606,-2006,-2404,-2801,-3196,-3590,-3981,-4370,-4756,-5139,-5520,-5897,-6270,-6639,-7005,-7366,-7723,-8076,-8423,-8765,-9102,-9434,-9760,-10080,-10394,-10702,-11003,-11297,-11585,-11866,-12140,-12406,-12665,-12916,-13160,-13395,-13623,-13842,-14053,-14256,-14449,-14635,-14811,-14978,-15137,-15286,-15426,-15557,-15679,-15791,-15893,-15986,-16069,-16143,-16207,-16261,-16305,-16340,-16364,-16379,-16384,-16379,-16364,-16340,-16305,-16261,-16207,-16143,-16069,-15986,-15893,-15791,-15679,-15557,-15426,-15286,-15137,-14978,-14811,-14635,-14449,-14256,-14053,-13842,-13623,-13395,-13160,-12916,-12665,-12406,-12140,-11866,-11585,-11297,-11003,-10702,-10394,-10080,-9760,-9434,-9102,-8765,-8423,-8076,-7723,-7366,-7005,-6639,-6270,-5897,-5520,-5139,-4756,-4370,-3981,-3590,-3196,-2801,-2404,-2006,-1606,-1205,-804,-402,0,-201,-402,-603,-804,-1005,-1205,-1406,-1606,-1806,-2006,-2205,-2404,-2603,-2801,-2999,-3196,-3393,-3590,-3786,-3981,-4176,-4370,-4563,-4756,-4948,-5139,-5330,-5520,-5708,-5897,-6084,-6270,-6455,-6639,-6823,-7005,-7186,-7366,-7545,-7723,-7900,-8076,-8250,-8423,-8595,-8765,-8935,-9102,-9269,-9434,-9598,-9760,-9921,-10080,-10238,-10394,-10549,-10702,-10853,-11003,-11151,-11297,-11442,-11585,-11727,-11866,-12004,-12140,-12274,-12406,-12537,-12665,-12792,-12916,-13039,-13160,-13279,-13395,-13510,-13623,-13733,-13842,-13949,-14053,-14155,-14256,-14354,-14449,-14543,-14635,-14724,-14811,-14896,-14978,-15059,-15137,-15213,-15286,-15357,-15426,-15493,-15557,-15619,-15679,-15736,-15791,-15843,-15893,-15941,-15986,-16029,-16069,-16107,-16143,-16176,-16207,-16235,-16261,-16284,-16305,-16324,-16340,-16353,-16364,-16373,-16379,-16383,0,-603,-1205,-1806,-2404,-2999,-3590,-4176,-4756,-5330,-5897,-6455,-7005,-7545,-8076,-8595,-9102,-9598,-10080,-10549,-11003,-11442,-11866,-12274,-12665,-13039,-13395,-13733,-14053,-14354,-14635,-14896,-15137,-15357,-15557,-15736,-15893,-16029,-16143,-16235,-16305,-16353,-16379,-16383,-16364,-16324,-16261,-16176,-16069,-15941,-15791,-15619,-15426,-15213,-14978,-14724,-14449,-14155,-13842,-13510,-13160,-12792,-12406,-12004,-11585,-11151,-10702,-10238,-9760,-9269,-8765,-8250,-7723,-7186,-6639,-6084,-5520,-4948,-4370,-3786,-3196,-2603,-2006,-1406,-804,-201,402,1005,1606,2205,2801,3393,3981,4563,5139,5708,6270,6823,7366,7900,8423,8935,9434,9921,10394,10853,11297,11727,12140,12537,12916,13279,13623,13949,14256,14543,14811,15059,15286,15493,15679,15843,15986,16107,16207,16284,16340,16373,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0))
      val rom2_i_comm = rom2_i inLinks ((ctrl, ctrl.dout, 3, 0) ~> (rom2_i, rom2_i.addr))

      // Twiddle Factor ROM 3
      val rom3_r = Rom("stage3_twiddle_r", 16, Seq(16384,16305,16069,15679,15137,14449,13623,12665,11585,10394,9102,7723,6270,4756,3196,1606,0,-1606,-3196,-4756,-6270,-7723,-9102,-10394,-11585,-12665,-13623,-14449,-15137,-15679,-16069,-16305,16384,16364,16305,16207,16069,15893,15679,15426,15137,14811,14449,14053,13623,13160,12665,12140,11585,11003,10394,9760,9102,8423,7723,7005,6270,5520,4756,3981,3196,2404,1606,804,16384,16207,15679,14811,13623,12140,10394,8423,6270,3981,1606,-804,-3196,-5520,-7723,-9760,-11585,-13160,-14449,-15426,-16069,-16364,-16305,-15893,-15137,-14053,-12665,-11003,-9102,-7005,-4756,-2404,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384,16384))
      val rom3_r_comm = rom3_r inLinks ((ctrl, ctrl.dout, 6, 0) ~> (rom3_r, rom3_r.addr))
      val rom3_i = Rom("stage3_twiddle_i", 16, Seq(0,-1606,-3196,-4756,-6270,-7723,-9102,-10394,-11585,-12665,-13623,-14449,-15137,-15679,-16069,-16305,-16384,-16305,-16069,-15679,-15137,-14449,-13623,-12665,-11585,-10394,-9102,-7723,-6270,-4756,-3196,-1606,0,-804,-1606,-2404,-3196,-3981,-4756,-5520,-6270,-7005,-7723,-8423,-9102,-9760,-10394,-11003,-11585,-12140,-12665,-13160,-13623,-14053,-14449,-14811,-15137,-15426,-15679,-15893,-16069,-16207,-16305,-16364,0,-2404,-4756,-7005,-9102,-11003,-12665,-14053,-15137,-15893,-16305,-16364,-16069,-15426,-14449,-13160,-11585,-9760,-7723,-5520,-3196,-804,1606,3981,6270,8423,10394,12140,13623,14811,15679,16207,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0))
      val rom3_i_comm = rom3_i inLinks ((ctrl, ctrl.dout, 3, 0) ~> (rom3_i, rom3_i.addr))

      // Twiddle Factor ROM 4
      val rom4_r = Rom("stage4_twiddle_r", 16, Seq(16384,15137,11585,6270,0,-6270,-11585,-15137,16384,16069,15137,13623,11585,9102,6270,3196,16384,13623,6270,-3196,-11585,-16069,-15137,-9102,16384,16384,16384,16384,16384,16384,16384,16384))
      val rom4_r_comm = rom4_r inLinks ((ctrl, ctrl.dout, 4, 0) ~> (rom4_r, rom4_r.addr))
      val rom4_i = Rom("stage4_twiddle_i", 16, Seq(0,-6270,-11585,-15137,-16384,-15137,-11585,-6270,0,-3196,-6270,-9102,-11585,-13623,-15137,-16069,0,-9102,-15137,-16069,-11585,-3196,6270,13623,0,0,0,0,0,0,0,0))
      val rom4_i_comm = rom4_i inLinks ((ctrl, ctrl.dout, 3, 0) ~> (rom4_i, rom4_i.addr))

      // Twiddle Factor ROM 5
      val rom5_r = Rom("stage5_twiddle_r", 16, Seq(16384,0,16384,11585,16384,-11585,16384,16384))
      val rom5_r_comm = rom5_r inLinks ((ctrl, ctrl.dout, 2, 0) ~> (rom5_r, rom5_r.addr))
      val rom5_i = Rom("stage5_twiddle_i", 16, Seq(0,-16384,0,-11585,0,-11585,0,0))
      val rom5_i_comm = rom5_i inLinks ((ctrl, ctrl.dout, 3, 0) ~> (rom5_i, rom5_i.addr))

      // -- stages
      // stage 1
      val fs1 = Stage("first", w, 1024, 512)
      val fs1_comm = fs1 inLinks(en ~> (fs1, fs1.en), (ctrl, ctrl.dout, 10) ~> (fs1, fs1.s1), (ctrl, ctrl.dout, 9) ~> (fs1, fs1.s2), (rom1_r, rom1_r.dout) ~> (fs1, fs1.tfr), (rom1_i, rom1_i.dout) ~> (fs1, fs1.tfi), xnr ~> (fs1, fs1.dinr), xni ~> (fs1, fs1.dini))

      // stage 2
      val fs2 = Stage("sec", w + 2, 256, 128)
      val fs2_comm = fs2 inLinks(en ~> (fs2, fs2.en), (ctrl, ctrl.dout, 8) ~> (fs2, fs2.s1), (ctrl, ctrl.dout, 7) ~> (fs2, fs2.s2), (rom2_r, rom2_r.dout) ~> (fs2, fs2.tfr), (rom2_i, rom2_i.dout) ~> (fs2, fs2.tfi), (fs1, fs1.doutr) ~> (fs2, fs2.dinr), (fs1, fs1.douti) ~> (fs2, fs2.dini))

      // stage 3
      val fs3 = Stage("third", w + 4, 64, 32)
      val fs3_comm = fs3 inLinks(en ~> (fs3, fs3.en), (ctrl, ctrl.dout, 6) ~> (fs3, fs3.s1), (ctrl, ctrl.dout, 5) ~> (fs3, fs3.s2), (rom3_r, rom3_r.dout) ~> (fs3, fs3.tfr), (rom3_i, rom3_i.dout) ~> (fs3, fs3.tfi), (fs2, fs2.doutr) ~> (fs3, fs3.dinr), (fs2, fs2.douti) ~> (fs3, fs3.dini))

      // stage 4
      val fs4 = Stage("fourth", w + 6, 16, 8)
      val fs4_comm = fs4 inLinks(en ~> (fs4, fs4.en), (ctrl, ctrl.dout, 4) ~> (fs4, fs4.s1), (ctrl, ctrl.dout, 3) ~> (fs4, fs4.s2), (rom4_r, rom4_r.dout) ~> (fs4, fs4.tfr), (rom4_i, rom4_i.dout) ~> (fs4, fs4.tfi), (fs3, fs3.doutr) ~> (fs4, fs4.dinr), (fs3, fs3.douti) ~> (fs4, fs4.dini))

      // stage 5
      val fs5 = Stage("fifth", w + 8, 4, 2)
      val fs5_comm = fs5 inLinks(en ~> (fs5, fs5.en), (ctrl, ctrl.dout, 2) ~> (fs5, fs5.s1), (ctrl, ctrl.dout, 1) ~> (fs5, fs5.s2), (rom5_r, rom5_r.dout) ~> (fs5, fs5.tfr), (rom5_i, rom5_i.dout) ~> (fs5, fs5.tfi), (fs4, fs4.doutr) ~> (fs5, fs5.dinr), (fs4, fs4.douti) ~> (fs5, fs5.dini))

      // last stage
      val ls = LastOddStage("last", w + 10, 1)
      val ls_comm = ls inLinks(en ~> (ls, ls.en), (ctrl, ctrl.dout, 0) ~> (ls, ls.s), (fs5, fs5.doutr) ~> (ls, ls.dinr), (fs5, fs5.douti) ~> (ls, ls.dini)) outLinks((ls, ls.doutr) ~> xkr, (ls, ls.douti) ~> xki)
      :=(ctrl_comm, rom1_r_comm, rom1_i_comm, rom2_r_comm, rom2_i_comm,rom3_r_comm, rom3_i_comm, rom4_r_comm, rom4_i_comm, rom5_r_comm, rom5_i_comm, fs1_comm, fs2_comm, fs3_comm, fs4_comm, fs5_comm, ls_comm)
    }

    override val name: String = "fft_n2048"
    override val width: Int = w
    override val dfg = model(Seq(cmb))
  }

  case class FFT_N2048(inst: String, w: Int) extends Module {
    //inputs
    val (en, xnr, xni) = (Streamer("en", 1), Streamer("xnr", w), Streamer("xni", w))
    // outputs
    val (xkr, xki) = (Streamer("xkr", w + 11), Streamer("xki", w + 11))

    override val iopaths = List((xnr, xkr))

    val fft = FFT2048("fft2048_Inst", w)
    val fft_comm = fft inLinks(en ~> (fft, fft.en), xnr ~> (fft, fft.xnr), xni ~> (fft, fft.xni)) outLinks((fft, fft.xkr) ~> xkr, (fft, fft.xki) ~> xki)

    override val name: String = fft.name
    override val width: Int = w
    override val dfg = fft.dfg
  }

  case class IFFT_N2048(inst: String, w: Int) extends Module {
    //inputs
    val (en, xnr, xni) = (Streamer("en", 1), Streamer("xnr", w), Streamer("xni", w))
    // outputs
    val (xkr, xki) = (Streamer("xkr", w + 11), Streamer("xki", w + 11))

    override val iopaths = List((xnr, xkr))

    val fft = FFT2048("fft2048_Inst", w)
    val fft_comm = fft inLinks(en ~> (fft, fft.en), xnr ~> (fft, fft.xni), xni ~> (fft, fft.xnr)) outLinks((fft, fft.xki) ~> xkr, (fft, fft.xkr) ~> xki)

    override val name: String = "ifft_n2048"
    override val width: Int = w
    override val dfg = model(Seq(fft_comm))
  }
}
