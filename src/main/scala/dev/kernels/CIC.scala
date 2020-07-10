package apps.kernels

import exp.CompExp.{Comb, Constants, Delay, DownSampler, Integrator}
import exp.KernelExp.Module
import exp.NodeExp.Streamer
import exp.PatternsExp.{Chain, FoldR, ZipWith}

import scala.collection.Seq

object CIC {

  case class CIC_4_Stages(inst: String, w: Int, m: Int, r: Int) extends Module {
    // input node x and output node y
    val (x, y) = (Streamer("din", w), Streamer("dout", w))
    override val iopaths = List((x, y))
    // declare integrators
    val (igt1, igt2, igt3, igt4) = (Integrator("igt1", w, w), Integrator("igt2", w, w), Integrator("igt3", w, w), Integrator("igt4", w, w))
    // declare combs
    val (cmb1, cmb2, cmb3, cmb4) = (Comb("cmb1", w, m), Comb("cmb2", w, m), Comb("cmb3", w, m), Comb("cmb4", w, m))
    // declare down-sampler
    val down_sampler = DownSampler("ds", w, r)
    // chain up the integrator template nodes
    val igt_chain = Chain(igt1 outLinks (igt1.dout ~> igt2.din, igt1.vld ~> igt2.en), igt2 outLinks (igt2.dout ~> igt3.din, igt2.vld ~> igt3.en), igt3 outLinks (igt3.dout ~> igt4.din, igt3.vld ~> igt4.en), igt4)
    // chain up the comb template nodes
    val cmb_chain = Chain(cmb1 outLinks (cmb1.dout ~> cmb2.din, cmb1.vld ~> cmb2.en), cmb2 outLinks (cmb2.dout ~> cmb3.din, cmb2.vld ~> cmb3.en), cmb3 outLinks (cmb3.dout ~> cmb4.din), cmb4)
    // chain them all up
    val dec_chain = Chain(igt_chain.out outLinks(igt_chain.out.asInstanceOf[Integrator].dout ~> down_sampler.din), down_sampler outLinks(down_sampler.dout ~> cmb_chain.in.asInstanceOf[Comb].din), cmb_chain.in)

    override val dfg = model(
      Seq(
        x ~> (igt1, igt1.din),
        igt_chain,
        cmb_chain,
        dec_chain,
        (igt4, igt4.vld) ~> (down_sampler, down_sampler.en),
        (down_sampler, down_sampler.vld) ~> (cmb4, cmb4.en),
        (cmb4, cmb4.dout) ~> y
      ))
    // filter module name
    override val name: String = "decimator"
    override val input_length: Int = 1
    override val output_length: Int = 1
  }

}
