package sdrlift.apps

import scalax.collection.GraphPredef._
import scalax.collection.Graph
import sdrlift.model.{Actor, Channel}
import Channel.ImplicitEdge
import exp.KernelExp.KernelPort
import sdrlift.graph.NodeFactory.ActorTypeEnum

object IR3Apps {

  /**
    * IEEE 802.11a Transmitter
    *
    * @return tx80211a-graph
    */
  def tx80211a: Graph[Actor, Channel] = {
    //////////////////////// IEEE 802.11a TX /////////////////////////////////////////////////////////////////////
    val zeropadParams = new java.util.HashMap[String, Any]()
    zeropadParams.put("dWidth", 16)
    zeropadParams.put("iSamples", 48)
    zeropadParams.put("padLength", 16)

    val ifftParams = new java.util.HashMap[String, Any]()
    ifftParams.put("N", 64)
    ifftParams.put("DIN_WIDTH", 16)
    ifftParams.put("DOUT_WIDTH", 22)
    ifftParams.put("MODE", '1')

    val cpParams = new java.util.HashMap[String, Any]()
    cpParams.put("dWidth", 22)
    cpParams.put("iSamples", 64)
    cpParams.put("prefixLength", 16)

    val (source, qam, zeropad, ifft, cp, sink) = (
      Actor("source", "sourceInst", null, 1, null),
      Actor("qammod", "qamInst", null, 3, null),
      Actor("zeropad", "zeropadInst", zeropadParams, 65, null),
      Actor("r22sdf_fft_ifft_core", "ifftInst", ifftParams, 128, null),
      Actor("cpadd", "cpInst", cpParams, 129, null),
      Actor("sink", "sinkInst", null, 1, null))


    //////////////////////////// LABELS  /////////////////////////////////
    val qamOutLabels = new java.util.HashMap[String, String]()
    qamOutLabels.put("dout", "Iout,Qout")

    val zeropadInLabels = new java.util.HashMap[String, String]()
    zeropadInLabels.put("din", "Iin,Qin")
    val zeropadOutLabels = new java.util.HashMap[String, String]()
    zeropadOutLabels.put("dout", "Iout,Qout")

    val ifftInLabels = new java.util.HashMap[String, String]()
    ifftInLabels.put("din", "XSr,XSi")
    val ifftOutLabels = new java.util.HashMap[String, String]()
    ifftOutLabels.put("dout", "XKr,XKi")

    val cpInLabels = new java.util.HashMap[String, String]()
    cpInLabels.put("din", "Iin,Qin")
    val cpOutLabels = new java.util.HashMap[String, String]()
    cpOutLabels.put("dout", "Iout,Qout")


    val sinkInLabels = new java.util.HashMap[String, String]()
    sinkInLabels.put("din", "iin,qin")

    val sinkOutLabels = new java.util.HashMap[String, String]()
    sinkOutLabels.put("dout", "iout,qout")

    Graph[Actor, Channel](
      source ~> qam ## ("ch1", KernelPort(1, List((1, "1")), null),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), null), 0, null),
      qam ~> zeropad ## ("ch2_i", KernelPort(1, List((2, "0"), (1, "1")), qamOutLabels),
        KernelPort(48, List((48, "1"), (17, "0")), zeropadInLabels), 0, ("Iout", "Iin")),
      qam ~> zeropad ## ("ch2_q", KernelPort(1, List((2, "0"), (1, "1")), qamOutLabels),
        KernelPort(48, List((48, "1"), (17, "0")), zeropadInLabels), 0, ("Qout", "Qin")),
      zeropad ~> ifft ## ("ch3_i", KernelPort(64, List((1, "0"), (64, "1")), zeropadOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), ifftInLabels), 0, ("Iout", "XSr")),
      zeropad ~> ifft ## ("ch3_q", KernelPort(64, List((1, "0"), (64, "1")), zeropadOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), ifftInLabels), 0, ("Qout", "XSi")),
      ifft ~> cp ## ("ch4_i", KernelPort(64, List((64, "0"), (64, "1")), ifftOutLabels),
        KernelPort(64, List((64, "1"), (65, "0")), cpInLabels), 0, ("XKr", "Iin")),
      ifft ~> cp ## ("ch4_q", KernelPort(64, List((64, "0"), (64, "1")), ifftOutLabels),
        KernelPort(64, List((64, "1"), (65, "0")), cpInLabels), 0, ("XKi", "Qin")),
      cp ~> sink ## ("ch5_i", KernelPort(80, List((49, "0"), (80, "1")), cpOutLabels),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("Iout", "Iin")),
      cp ~> sink ## ("ch5_q", KernelPort(80, List((49, "0"), (80, "1")), cpOutLabels),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("Qout", "Qin")))
  }

  /**
    * IEEE 802.11a Receiver
    *
    * @return rx80211a-graph
    */
  def rx80211a: Graph[Actor, Channel] = {
    //////////////////////////// IEEE 802.11a RX /////////////////////////////////
    val sourceParams = new java.util.HashMap[String, Any]()
    sourceParams.put("dWidth", 16)

    val cpParams = new java.util.HashMap[String, Any]()
    cpParams.put("dWidth", 16)
    cpParams.put("oSamples", 64)
    cpParams.put("prefixLength", 16)

    val fftParams = new java.util.HashMap[String, Any]()
    fftParams.put("N", 64)
    fftParams.put("DIN_WIDTH", 16)
    fftParams.put("DOUT_WIDTH", 22)
    fftParams.put("MODE", '0')

    val zerocutParams = new java.util.HashMap[String, Any]()
    zerocutParams.put("dWidth", 22)
    zerocutParams.put("oSamples", 48)
    zerocutParams.put("padLength", 16)

    val qamdemodParams = new java.util.HashMap[String, Any]()
    qamdemodParams.put("iWidth", 22)
    qamdemodParams.put("oWidth", 22)

    val (source, cp, fft, zerocut, qamdemod, sink) = (Actor("source2", "sourceInst", sourceParams, 1, null),
      Actor("cpremove", "cpInst", cpParams, 81, null),
      Actor("r22sdf_fft_ifft_core", "fftInst", fftParams, 128, null),
      Actor("zerocut", "zerocutInst", zerocutParams, 65, null),
      Actor("qamdemod", "qamdemodInst", qamdemodParams, 3, null),
      Actor("sink2", "sinkInst", null, 1, null))


    //////////////////////////// LABELS  /////////////////////////////////

    val sourceInLabels = new java.util.HashMap[String, String]()
    sourceInLabels.put("din", "Iin,Qin")
    val sourceOutLabels = new java.util.HashMap[String, String]()
    sourceOutLabels.put("dout", "Iout,Qout")

    val cpInLabels = new java.util.HashMap[String, String]()
    cpInLabels.put("din", "Iin,Qin")
    val cpOutLabels = new java.util.HashMap[String, String]()
    cpOutLabels.put("dout", "Iout,Qout")

    val fftInLabels = new java.util.HashMap[String, String]()
    fftInLabels.put("din", "XSr,XSi")
    val fftOutLabels = new java.util.HashMap[String, String]()
    fftOutLabels.put("dout", "XKr,XKi")

    val zerocutInLabels = new java.util.HashMap[String, String]()
    zerocutInLabels.put("din", "Iin,Qin")
    val zerocutOutLabels = new java.util.HashMap[String, String]()
    zerocutOutLabels.put("dout", "Iout,Qout")

    val qamdemodInLabels = new java.util.HashMap[String, String]()
    qamdemodInLabels.put("din", "Iin,Qin")

    Graph[Actor, Channel](
      source ~> cp ## ("ch1_i", KernelPort(1, List((1, "1")), sourceOutLabels),
        KernelPort(80, List((80, "1"), (1, "0")), cpInLabels), 0, ("Iout", "Iin")),
      source ~> cp ## ("ch1_q", KernelPort(1, List((1, "1")), sourceOutLabels),
        KernelPort(80, List((80, "1"), (1, "0")), cpInLabels), 0, ("Qout", "Qin")),
      cp ~> fft ## ("ch2_i", KernelPort(64, List((17, "0"), (64, "1")), cpOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), fftInLabels), 0, ("Iout", "XSr")),
      cp ~> fft ## ("ch2_q", KernelPort(64, List((17, "0"), (64, "1")), cpOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), fftInLabels), 0, ("Qout", "XSi")),
      fft ~> zerocut ## ("ch3_i", KernelPort(64, List((64, "0"), (64, "1")), fftOutLabels),
        KernelPort(64, List((64, "1"), (1, "0")), zerocutInLabels), 0, ("XKr", "Iin")),
      fft ~> zerocut ## ("ch3_q", KernelPort(64, List((64, "0"), (64, "1")), fftOutLabels),
        KernelPort(64, List((64, "1"), (1, "0")), zerocutInLabels), 0, ("XKi", "Qin")),
      zerocut ~> qamdemod ## ("ch4_i", KernelPort(48, List((1, "0"), (48, "1"), (16, "0")), zerocutOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), qamdemodInLabels), 0, ("Iout", "Iin")),
      zerocut ~> qamdemod ## ("ch4_q", KernelPort(48, List((1, "0"), (48, "1"), (16, "0")), zerocutOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), qamdemodInLabels), 0, ("Qout", "Qin")),
      qamdemod ~> sink ## ("ch5", KernelPort(1, List((1, "0"), (1, "0"), (1, "1")), null),
        KernelPort(1, List((1, "1")), null), 0, null))
  }

  /**
    * IEEE tx802.22 Transmitter
    *
    * @return tx80222-graph
    */
  def tx80222: Graph[Actor, Channel] = {
    val zeropadParams = new java.util.HashMap[String, Any]()
    zeropadParams.put("dWidth", 16)
    zeropadParams.put("iSamples", 1200)
    zeropadParams.put("padLength", 848)

    val ifftParams = new java.util.HashMap[String, Any]()
    ifftParams.put("N", 2048)
    ifftParams.put("DIN_WIDTH", 16)
    ifftParams.put("DOUT_WIDTH", 16) //27
    ifftParams.put("MODE", '0')

    val cpParams = new java.util.HashMap[String, Any]()
    cpParams.put("dWidth", 16) //27
    cpParams.put("iSamples", 2048)
    cpParams.put("prefixLength", 512)

    val sinkParams = new java.util.HashMap[String, Any]()
    sinkParams.put("dWidth", 16) //27

    val (source, qam, zeropad, ifft, cp, sink) = (
      Actor("source", "sourceInst", null, 1, null),
      Actor("qammod", "qamInst", null, 3, null),
      Actor("zeropad", "zeropadInst", zeropadParams, 2049, null),
      Actor("r22sdf_fft_ifft_core", "ifftInst", ifftParams, 4096, null),
      Actor("cpadd", "cpInst", cpParams, 4097, null),
      Actor("sink", "sinkInst", sinkParams, 1, null))


    //////////////////////////// LABELS  /////////////////////////////////
    val qamOutLabels = new java.util.HashMap[String, String]()
    qamOutLabels.put("dout", "Iout,Qout")

    val zeropadInLabels = new java.util.HashMap[String, String]()
    zeropadInLabels.put("din", "Iin,Qin")
    val zeropadOutLabels = new java.util.HashMap[String, String]()
    zeropadOutLabels.put("dout", "Iout,Qout")

    val ifftInLabels = new java.util.HashMap[String, String]()
    ifftInLabels.put("din", "XSr,XSi")
    val ifftOutLabels = new java.util.HashMap[String, String]()
    ifftOutLabels.put("dout", "XKr,XKi")

    val cpInLabels = new java.util.HashMap[String, String]()
    cpInLabels.put("din", "Iin,Qin")
    val cpOutLabels = new java.util.HashMap[String, String]()
    cpOutLabels.put("dout", "Iout,Qout")

    val sinkInLabels = new java.util.HashMap[String, String]()
    sinkInLabels.put("din", "iin,qin")

    val sinkOutLabels = new java.util.HashMap[String, String]()
    sinkOutLabels.put("dout", "iout,qout")

    Graph[Actor, Channel](
      source ~> qam ## ("ch1", KernelPort(1, List((1, "1")), null),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), null), 0, null),
      qam ~> zeropad ## ("ch2_i", KernelPort(1, List((2, "0"), (1, "1")), qamOutLabels),
        KernelPort(1200, List((1200, "1"), (849, "0")), zeropadInLabels), 0, ("Iout", "Iin")),
      qam ~> zeropad ## ("ch2_q", KernelPort(1, List((2, "0"), (1, "1")), qamOutLabels),
        KernelPort(1200, List((1200, "1"), (849, "0")), zeropadInLabels), 0, ("Qout", "Qin")),
      zeropad ~> ifft ## ("ch3_i", KernelPort(2048, List((1, "0"), (2048, "1")), zeropadOutLabels),
        KernelPort(2048, List((2048, "1"), (2048, "0")), ifftInLabels), 0, ("Iout", "XSr")),
      zeropad ~> ifft ## ("ch3_q", KernelPort(2048, List((1, "0"), (2048, "1")), zeropadOutLabels),
        KernelPort(2048, List((2048, "1"), (2048, "0")), ifftInLabels), 0, ("Qout", "XSi")),
      ifft ~> cp ## ("ch4_i", KernelPort(2048, List((2048, "0"), (2048, "1")), ifftOutLabels),
        KernelPort(2048, List((2048, "1"), (2049, "0")), cpInLabels), 0, ("XKr", "Iin")),
      ifft ~> cp ## ("ch4_q", KernelPort(2048, List((2048, "0"), (2048, "1")), ifftOutLabels),
        KernelPort(2048, List((2048, "1"), (2049, "0")), cpInLabels), 0, ("XKi", "Qin")),
      cp ~> sink ## ("ch5_i", KernelPort(2560, List((1537, "0"), (2560, "1")), cpOutLabels),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("Iout", "Iin")),
      cp ~> sink ## ("ch5_q", KernelPort(2560, List((1537, "0"), (2560, "1")), cpOutLabels),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("Qout", "Qin")))
  }

  /**
    * IEEE rx802.22 Receiver
    *
    * @return rx80222-graph
    */
  def rx80222: Graph[Actor, Channel] = {
    val sourceParams = new java.util.HashMap[String, Any]()
    sourceParams.put("dWidth", 16)

    val zerocutParams = new java.util.HashMap[String, Any]()
    zerocutParams.put("dWidth", 16) // 27
    zerocutParams.put("oSamples", 1200)
    zerocutParams.put("padLength", 848)

    val fftParams = new java.util.HashMap[String, Any]()
    fftParams.put("N", 2048)
    fftParams.put("DIN_WIDTH", 16)
    fftParams.put("DOUT_WIDTH", 16) // 27
    fftParams.put("MODE", '0')

    val cpParams = new java.util.HashMap[String, Any]()
    cpParams.put("dWidth", 16)
    cpParams.put("oSamples", 2048)
    cpParams.put("prefixLength", 512)

    val gainParams = new java.util.HashMap[String, Any]()
    gainParams.put("dwidth", 16)
    gainParams.put("divisor", 8)

    val qamdemodParams = new java.util.HashMap[String, Any]()
    qamdemodParams.put("iWidth", 16) //27
    qamdemodParams.put("oWidth", 16) // 54

    val sinkParams = new java.util.HashMap[String, Any]()
    sinkParams.put("dWidth", 16)

    val (source, cp, fft, zerocut, qamdemod, sink) = (Actor("source2", "sourceInst", sourceParams, 1, null),
      Actor("cpremove", "cpInst", cpParams, 2561, null),
      Actor("r22sdf_fft_ifft_core", "fftInst", fftParams, 4096, null),
      Actor("zerocut", "zerocutInst", zerocutParams, 2049, null),
      Actor("qamdemod", "qamdemodInst", qamdemodParams, 3, null),
      Actor("sink2", "sinkInst", sinkParams, 1, null))


    //////////////////////////// LABELS  /////////////////////////////////

    val sourceInLabels = new java.util.HashMap[String, String]()
    sourceInLabels.put("din", "Iin,Qin")
    val sourceOutLabels = new java.util.HashMap[String, String]()
    sourceOutLabels.put("dout", "Iout,Qout")

    val cpInLabels = new java.util.HashMap[String, String]()
    cpInLabels.put("din", "Iin,Qin")
    val cpOutLabels = new java.util.HashMap[String, String]()
    cpOutLabels.put("dout", "Iout,Qout")

    val fftInLabels = new java.util.HashMap[String, String]()
    fftInLabels.put("din", "XSr,XSi")
    val fftOutLabels = new java.util.HashMap[String, String]()
    fftOutLabels.put("dout", "XKr,XKi")

    val zerocutInLabels = new java.util.HashMap[String, String]()
    zerocutInLabels.put("din", "Iin,Qin")
    val zerocutOutLabels = new java.util.HashMap[String, String]()
    zerocutOutLabels.put("dout", "Iout,Qout")

    val qamdemodInLabels = new java.util.HashMap[String, String]()
    qamdemodInLabels.put("din", "Iin,Qin")

    Graph[Actor, Channel](
      source ~> cp ## ("ch1_i", KernelPort(1, List((1, "1")), sourceOutLabels),
        KernelPort(2560, List((2560, "1"), (1, "0")), cpInLabels), 0, ("Iout", "Iin")),
      source ~> cp ## ("ch1_q", KernelPort(1, List((1, "1")), sourceOutLabels),
        KernelPort(2560, List((2560, "1"), (1, "0")), cpInLabels), 0, ("Qout", "Qin")),
      cp ~> fft ## ("ch2_i", KernelPort(2048, List((513, "0"), (2048, "1")), cpOutLabels),
        KernelPort(2048, List((2048, "1"), (2048, "0")), fftInLabels), 0, ("Iout", "XSr")),
      cp ~> fft ## ("ch2_q", KernelPort(2048, List((513, "0"), (2048, "1")), cpOutLabels),
        KernelPort(2048, List((2048, "1"), (2048, "0")), fftInLabels), 0, ("Qout", "XSi")),
      fft ~> zerocut ## ("ch3_i", KernelPort(2048, List((2048, "0"), (2048, "1")), fftOutLabels),
        KernelPort(2048, List((2048, "1"), (1, "0")), zerocutInLabels), 0, ("XKr", "Iin")),
      fft ~> zerocut ## ("ch3_q", KernelPort(2048, List((2048, "0"), (2048, "1")), fftOutLabels),
        KernelPort(2048, List((2048, "1"), (1, "0")), zerocutInLabels), 0, ("XKi", "Qin")),
      zerocut ~> qamdemod ## ("ch4_i", KernelPort(1200, List((1, "0"), (1200, "1"), (848, "0")), zerocutOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), qamdemodInLabels), 0, ("Iout", "Iin")),
      zerocut ~> qamdemod ## ("ch4_q", KernelPort(1200, List((1, "0"), (1200, "1"), (848, "0")), zerocutOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), qamdemodInLabels), 0, ("Qout", "Qin")),
      qamdemod ~> sink ## ("ch5", KernelPort(1, List((1, "0"), (1, "0"), (1, "1")), null),
        KernelPort(1, List((1, "1")), null), 0, null))
  }


  /**
    * 4-KernelPort MIMO 802.11a Transmitter
    *
    * @return mimotx80211a-graph
    */
  def mimotx80211a: Graph[Actor, Channel] = {
    val zeropadParams = new java.util.HashMap[String, Any]()
    zeropadParams.put("dWidth", 16)
    zeropadParams.put("iSamples", 48)
    zeropadParams.put("padLength", 16)

    val ifftParams = new java.util.HashMap[String, Any]()
    ifftParams.put("N", 64)
    ifftParams.put("DIN_WIDTH", 16)
    ifftParams.put("DOUT_WIDTH", 22)
    ifftParams.put("MODE", '1')

    val cpParams = new java.util.HashMap[String, Any]()
    cpParams.put("dWidth", 22)
    cpParams.put("iSamples", 64)
    cpParams.put("prefixLength", 16)

    val spParams = new java.util.HashMap[String, Any]()
    spParams.put("dWidth", 4)

    val (source, sp, qam1, zeropad1, ifft1, cp1, sink1) = (
      Actor("source", "sourceInst1", null, 1, null),
      Actor("sp", "spInst1", spParams, 240, null),
      Actor("qammod", "qamInst1", null, 3, null),
      Actor("zeropad", "zeropadInst1", zeropadParams, 65, null),
      Actor("r22sdf_fft_ifft_core", "ifftInst1", ifftParams, 128, null),
      Actor("cpadd", "cpInst1", cpParams, 129, null),
      Actor("sink", "sinkInst1", null, 1, null))

    val (qam2, zeropad2, ifft2, cp2, sink2) = (
      Actor("qammod", "qamInst2", null, 3, null),
      Actor("zeropad", "zeropadInst2", zeropadParams, 65, null),
      Actor("r22sdf_fft_ifft_core", "ifftInst2", ifftParams, 128, null),
      Actor("cpadd", "cpInst2", cpParams, 129, null),
      Actor("sink", "sinkInst2", null, 1, null))

    val (qam3, zeropad3, ifft3, cp3, sink3) = (
      Actor("qammod", "qamInst3", null, 3, null),
      Actor("zeropad", "zeropadInst3", zeropadParams, 65, null),
      Actor("r22sdf_fft_ifft_core", "ifftInst3", ifftParams, 128, null),
      Actor("cpadd", "cpInst3", cpParams, 129, null),
      Actor("sink", "sinkInst3", null, 1, null))

    val (qam4, zeropad4, ifft4, cp4, sink4) = (
      Actor("qammod", "qamInst4", null, 3, null),
      Actor("zeropad", "zeropadInst4", zeropadParams, 65, null),
      Actor("r22sdf_fft_ifft_core", "ifftInst4", ifftParams, 128, null),
      Actor("cpadd", "cpInst4", cpParams, 129, null),
      Actor("sink", "sinkInst4", null, 1, null))

    //////////////////////////// LABELS  /////////////////////////////////
    val qamOutLabels = new java.util.HashMap[String, String]()
    qamOutLabels.put("dout", "Iout,Qout")

    val zeropadInLabels = new java.util.HashMap[String, String]()
    zeropadInLabels.put("din", "Iin,Qin")
    val zeropadOutLabels = new java.util.HashMap[String, String]()
    zeropadOutLabels.put("dout", "Iout,Qout")

    val ifftInLabels = new java.util.HashMap[String, String]()
    ifftInLabels.put("din", "XSr,XSi")
    val ifftOutLabels = new java.util.HashMap[String, String]()
    ifftOutLabels.put("dout", "XKr,XKi")

    val cpInLabels = new java.util.HashMap[String, String]()
    cpInLabels.put("din", "Iin,Qin")
    val cpOutLabels = new java.util.HashMap[String, String]()
    cpOutLabels.put("dout", "Iout,Qout")


    val sinkInLabels = new java.util.HashMap[String, String]()
    sinkInLabels.put("din", "iin,qin")

    val sinkOutLabels = new java.util.HashMap[String, String]()
    sinkOutLabels.put("dout", "iout,qout")

    val spOutLabels = new java.util.HashMap[String, String]()
    spOutLabels.put("dout", "dout1,dout2,dout3,dout4")


    Graph[Actor, Channel](
      source ~> sp ## ("ch0", KernelPort(1, List((1, "1")), null),
        KernelPort(192, List((48, "11110")), null), 0, null),

      // Channel 0
      sp ~> qam1 ## ("ch1", KernelPort(48, List((48, "00001")), spOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), null), 0, ("dout1", "din")),
      qam1 ~> zeropad1 ## ("ch1_2_i", KernelPort(1, List((2, "0"), (1, "1")), qamOutLabels),
        KernelPort(48, List((48, "1"), (17, "0")), zeropadInLabels), 0, ("Iout", "Iin")),
      qam1 ~> zeropad1 ## ("ch1_2_q", KernelPort(1, List((2, "0"), (1, "1")), qamOutLabels),
        KernelPort(48, List((48, "1"), (17, "0")), zeropadInLabels), 0, ("Qout", "Qin")),
      zeropad1 ~> ifft1 ## ("ch1_3_i", KernelPort(64, List((1, "0"), (64, "1")), zeropadOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), ifftInLabels), 0, ("Iout", "XSr")),
      zeropad1 ~> ifft1 ## ("ch1_3_q", KernelPort(64, List((1, "0"), (64, "1")), zeropadOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), ifftInLabels), 0, ("Qout", "XSi")),
      ifft1 ~> cp1 ## ("ch1_4_i", KernelPort(64, List((64, "0"), (64, "1")), ifftOutLabels),
        KernelPort(64, List((64, "1"), (65, "0")), cpInLabels), 0, ("XKr", "Iin")),
      ifft1 ~> cp1 ## ("ch1_4_q", KernelPort(64, List((64, "0"), (64, "1")), ifftOutLabels),
        KernelPort(64, List((64, "1"), (65, "0")), cpInLabels), 0, ("XKi", "Qin")),
      cp1 ~> sink1 ## ("ch1_5_i", KernelPort(80, List((49, "0"), (80, "1")), cpOutLabels),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("Iout", "Iin")),
      cp1 ~> sink1 ## ("ch1_5_q", KernelPort(80, List((49, "0"), (80, "1")), cpOutLabels),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("Qout", "Qin")),

      // Channel 1
      sp ~> qam2 ## ("ch2", KernelPort(48, List((48, "00001")), spOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), null), 0, ("dout2", "din")),
      qam2 ~> zeropad2 ## ("ch2_2_i", KernelPort(1, List((2, "0"), (1, "1")), qamOutLabels),
        KernelPort(48, List((48, "1"), (17, "0")), zeropadInLabels), 0, ("Iout", "Iin")),
      qam2 ~> zeropad2 ## ("ch2_2_q", KernelPort(1, List((2, "0"), (1, "1")), qamOutLabels),
        KernelPort(48, List((48, "1"), (17, "0")), zeropadInLabels), 0, ("Qout", "Qin")),
      zeropad2 ~> ifft2 ## ("ch2_3_i", KernelPort(64, List((1, "0"), (64, "1")), zeropadOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), ifftInLabels), 0, ("Iout", "XSr")),
      zeropad2 ~> ifft2 ## ("ch2_3_q", KernelPort(64, List((1, "0"), (64, "1")), zeropadOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), ifftInLabels), 0, ("Qout", "XSi")),
      ifft2 ~> cp2 ## ("ch2_4_i", KernelPort(64, List((64, "0"), (64, "1")), ifftOutLabels),
        KernelPort(64, List((64, "1"), (65, "0")), cpInLabels), 0, ("XKr", "Iin")),
      ifft2 ~> cp2 ## ("ch2_4_q", KernelPort(64, List((64, "0"), (64, "1")), ifftOutLabels),
        KernelPort(64, List((64, "1"), (65, "0")), cpInLabels), 0, ("XKi", "Qin")),
      cp2 ~> sink2 ## ("ch2_5_i", KernelPort(80, List((49, "0"), (80, "1")), cpOutLabels),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("Iout", "Iin")),
      cp2 ~> sink2 ## ("ch2_5_q", KernelPort(80, List((49, "0"), (80, "1")), cpOutLabels),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("Qout", "Qin")),
      // Channel 2
      sp ~> qam3 ## ("ch3", KernelPort(48, List((48, "00001")), spOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), null), 0, ("dout3", "din")),
      qam3 ~> zeropad3 ## ("ch3_2_i", KernelPort(1, List((2, "0"), (1, "1")), qamOutLabels),
        KernelPort(48, List((48, "1"), (17, "0")), zeropadInLabels), 0, ("Iout", "Iin")),
      qam3 ~> zeropad3 ## ("ch3_2_q", KernelPort(1, List((2, "0"), (1, "1")), qamOutLabels),
        KernelPort(48, List((48, "1"), (17, "0")), zeropadInLabels), 0, ("Qout", "Qin")),
      zeropad3 ~> ifft3 ## ("ch3_3_i", KernelPort(64, List((1, "0"), (64, "1")), zeropadOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), ifftInLabels), 0, ("Iout", "XSr")),
      zeropad3 ~> ifft3 ## ("ch3_3_q", KernelPort(64, List((1, "0"), (64, "1")), zeropadOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), ifftInLabels), 0, ("Qout", "XSi")),
      ifft3 ~> cp3 ## ("ch3_4_i", KernelPort(64, List((64, "0"), (64, "1")), ifftOutLabels),
        KernelPort(64, List((64, "1"), (65, "0")), cpInLabels), 0, ("XKr", "Iin")),
      ifft3 ~> cp3 ## ("ch3_4_q", KernelPort(64, List((64, "0"), (64, "1")), ifftOutLabels),
        KernelPort(64, List((64, "1"), (65, "0")), cpInLabels), 0, ("XKi", "Qin")),
      cp3 ~> sink3 ## ("ch3_5_i", KernelPort(80, List((49, "0"), (80, "1")), cpOutLabels),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("Iout", "Iin")),
      cp3 ~> sink3 ## ("ch3_5_q", KernelPort(80, List((49, "0"), (80, "1")), cpOutLabels),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("Qout", "Qin")),
      // Channel 3
      sp ~> qam4 ## ("ch4", KernelPort(48, List((48, "00001")), spOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), null), 0, ("dout4", "din")),
      qam4 ~> zeropad4 ## ("ch4_2_i", KernelPort(1, List((2, "0"), (1, "1")), qamOutLabels),
        KernelPort(48, List((48, "1"), (17, "0")), zeropadInLabels), 0, ("Iout", "Iin")),
      qam4 ~> zeropad4 ## ("ch4_2_q", KernelPort(1, List((2, "0"), (1, "1")), qamOutLabels),
        KernelPort(48, List((48, "1"), (17, "0")), zeropadInLabels), 0, ("Qout", "Qin")),
      zeropad4 ~> ifft4 ## ("ch4_3_i", KernelPort(64, List((1, "0"), (64, "1")), zeropadOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), ifftInLabels), 0, ("Iout", "XSr")),
      zeropad4 ~> ifft4 ## ("ch4_3_q", KernelPort(64, List((1, "0"), (64, "1")), zeropadOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), ifftInLabels), 0, ("Qout", "XSi")),
      ifft4 ~> cp4 ## ("ch4_4_i", KernelPort(64, List((64, "0"), (64, "1")), ifftOutLabels),
        KernelPort(64, List((64, "1"), (65, "0")), cpInLabels), 0, ("XKr", "Iin")),
      ifft4 ~> cp4 ## ("ch4_4_q", KernelPort(64, List((64, "0"), (64, "1")), ifftOutLabels),
        KernelPort(64, List((64, "1"), (65, "0")), cpInLabels), 0, ("XKi", "Qin")),
      cp4 ~> sink4 ## ("ch4_5_i", KernelPort(80, List((49, "0"), (80, "1")), cpOutLabels),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("Iout", "Iin")),
      cp4 ~> sink4 ## ("ch4_5_q", KernelPort(80, List((49, "0"), (80, "1")), cpOutLabels),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("Qout", "Qin")))
  }

  /**
    * 4-KernelPort MIMO 802.11a Receiver
    *
    * @return mimorx80211a-graph
    */
  def mimorx80211a: Graph[Actor, Channel] = {
    val sourceParams = new java.util.HashMap[String, Any]()
    sourceParams.put("dWidth", 16)

    val cpParams = new java.util.HashMap[String, Any]()
    cpParams.put("dWidth", 16)
    cpParams.put("oSamples", 64)
    cpParams.put("prefixLength", 16)

    val fftParams = new java.util.HashMap[String, Any]()
    fftParams.put("N", 64)
    fftParams.put("DIN_WIDTH", 16)
    fftParams.put("DOUT_WIDTH", 22)
    fftParams.put("MODE", '0')

    val zerocutParams = new java.util.HashMap[String, Any]()
    zerocutParams.put("dWidth", 22)
    zerocutParams.put("oSamples", 48)
    zerocutParams.put("padLength", 16)

    val qamdemodParams = new java.util.HashMap[String, Any]()
    qamdemodParams.put("iWidth", 22)
    qamdemodParams.put("oWidth", 22)

    val psParams = new java.util.HashMap[String, Any]()
    psParams.put("dWidth", 22)

    val (source1, cp1, fft1, zerocut1, qamdemod1, ps, sink) = (Actor("source2", "sourceInst1", sourceParams, 1, null),
      Actor("cpremove", "cpInst1", cpParams, 81, null),
      Actor("r22sdf_fft_ifft_core", "fftInst1", fftParams, 128, null),
      Actor("zerocut", "zerocutInst1", zerocutParams, 65, null),
      Actor("qamdemod", "qamdemodInst1", qamdemodParams, 3, null),
      Actor("p2s", "psInst1", psParams, 240, null),
      Actor("sink2", "sinkInst1", null, 1, null))

    val (source2, cp2, fft2, zerocut2, qamdemod2) = (Actor("source2", "sourceInst2", sourceParams, 1, null),
      Actor("cpremove", "cpInst2", cpParams, 81, null),
      Actor("r22sdf_fft_ifft_core", "fftInst2", fftParams, 128, null),
      Actor("zerocut", "zerocutInst2", zerocutParams, 65, null),
      Actor("qamdemod", "qamdemodInst2", qamdemodParams, 3, null))

    val (source3, cp3, fft3, zerocut3, qamdemod3) = (Actor("source2", "sourceInst3", sourceParams, 1, null),
      Actor("cpremove", "cpInst3", cpParams, 81, null),
      Actor("r22sdf_fft_ifft_core", "fftInst3", fftParams, 128, null),
      Actor("zerocut", "zerocutInst3", zerocutParams, 65, null),
      Actor("qamdemod", "qamdemodInst3", qamdemodParams, 3, null))

    val (source4, cp4, fft4, zerocut4, qamdemod4) = (Actor("source2", "sourceInst4", sourceParams, 1, null),
      Actor("cpremove", "cpInst4", cpParams, 81, null),
      Actor("r22sdf_fft_ifft_core", "fftInst4", fftParams, 128, null),
      Actor("zerocut", "zerocutInst4", zerocutParams, 65, null),
      Actor("qamdemod", "qamdemodInst4", qamdemodParams, 3, null))


    //////////////////////////// LABELS  /////////////////////////////////

    val sourceInLabels = new java.util.HashMap[String, String]()
    sourceInLabels.put("din", "Iin,Qin")
    val sourceOutLabels = new java.util.HashMap[String, String]()
    sourceOutLabels.put("dout", "Iout,Qout")

    val cpInLabels = new java.util.HashMap[String, String]()
    cpInLabels.put("din", "Iin,Qin")
    val cpOutLabels = new java.util.HashMap[String, String]()
    cpOutLabels.put("dout", "Iout,Qout")

    val fftInLabels = new java.util.HashMap[String, String]()
    fftInLabels.put("din", "XSr,XSi")
    val fftOutLabels = new java.util.HashMap[String, String]()
    fftOutLabels.put("dout", "XKr,XKi")

    val zerocutInLabels = new java.util.HashMap[String, String]()
    zerocutInLabels.put("din", "Iin,Qin")
    val zerocutOutLabels = new java.util.HashMap[String, String]()
    zerocutOutLabels.put("dout", "Iout,Qout")

    val qamdemodInLabels = new java.util.HashMap[String, String]()
    qamdemodInLabels.put("din", "Iin,Qin")

    val sinkInLabels = new java.util.HashMap[String, String]()
    sinkInLabels.put("din", "iin,qin")

    val sinkOutLabels = new java.util.HashMap[String, String]()
    sinkOutLabels.put("dout", "iout,qout")

    val psInLabels = new java.util.HashMap[String, String]()
    psInLabels.put("din", "din1,din2,din3,din4")


    Graph[Actor, Channel](
      // Channel 0
      source1 ~> cp1 ## ("ch1_1_i", KernelPort(1, List((1, "1")), sourceOutLabels),
        KernelPort(80, List((80, "1"), (1, "0")), cpInLabels), 0, ("Iout", "Iin")),
      source1 ~> cp1 ## ("ch1_1_q", KernelPort(1, List((1, "1")), sourceOutLabels),
        KernelPort(80, List((80, "1"), (1, "0")), cpInLabels), 0, ("Qout", "Qin")),
      cp1 ~> fft1 ## ("ch1_2_i", KernelPort(64, List((17, "0"), (64, "1")), cpOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), fftInLabels), 0, ("Iout", "XSr")),
      cp1 ~> fft1 ## ("ch1_2_q", KernelPort(64, List((17, "0"), (64, "1")), cpOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), fftInLabels), 0, ("Qout", "XSi")),
      fft1 ~> zerocut1 ## ("ch1_3_i", KernelPort(64, List((64, "0"), (64, "1")), fftOutLabels),
        KernelPort(64, List((64, "1"), (1, "0")), zerocutInLabels), 0, ("XKr", "Iin")),
      fft1 ~> zerocut1 ## ("ch1_3_q", KernelPort(64, List((64, "0"), (64, "1")), fftOutLabels),
        KernelPort(64, List((64, "1"), (1, "0")), zerocutInLabels), 0, ("XKi", "Qin")),
      zerocut1 ~> qamdemod1 ## ("ch1_4_i", KernelPort(48, List((1, "0"), (48, "1"), (16, "0")), zerocutOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), qamdemodInLabels), 0, ("Iout", "Iin")),
      zerocut1 ~> qamdemod1 ## ("ch1_4_q", KernelPort(48, List((1, "0"), (48, "1"), (16, "0")), zerocutOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), qamdemodInLabels), 0, ("Qout", "Qin")),
      qamdemod1 ~> ps ## ("ch1_5", KernelPort(1, List((1, "0"), (1, "0"), (1, "1")), null),
        KernelPort(48, List((48, "10000")), psInLabels), 0, ("dout", "din1")),

      // Channel 1
      source2 ~> cp2 ## ("ch2_1_i", KernelPort(1, List((1, "1")), sourceOutLabels),
        KernelPort(80, List((80, "1"), (1, "0")), cpInLabels), 0, ("Iout", "Iin")),
      source2 ~> cp2 ## ("ch2_1_q", KernelPort(1, List((1, "1")), sourceOutLabels),
        KernelPort(80, List((80, "1"), (1, "0")), cpInLabels), 0, ("Qout", "Qin")),
      cp2 ~> fft2 ## ("ch2_2_i", KernelPort(64, List((17, "0"), (64, "1")), cpOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), fftInLabels), 0, ("Iout", "XSr")),
      cp2 ~> fft2 ## ("ch2_2_q", KernelPort(64, List((17, "0"), (64, "1")), cpOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), fftInLabels), 0, ("Qout", "XSi")),
      fft2 ~> zerocut2 ## ("ch2_3_i", KernelPort(64, List((64, "0"), (64, "1")), fftOutLabels),
        KernelPort(64, List((64, "1"), (1, "0")), zerocutInLabels), 0, ("XKr", "Iin")),
      fft2 ~> zerocut2 ## ("ch2_3_q", KernelPort(64, List((64, "0"), (64, "1")), fftOutLabels),
        KernelPort(64, List((64, "1"), (1, "0")), zerocutInLabels), 0, ("XKi", "Qin")),
      zerocut2 ~> qamdemod2 ## ("ch2_4_i", KernelPort(48, List((1, "0"), (48, "1"), (16, "0")), zerocutOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), qamdemodInLabels), 0, ("Iout", "Iin")),
      zerocut2 ~> qamdemod2 ## ("ch2_4_q", KernelPort(48, List((1, "0"), (48, "1"), (16, "0")), zerocutOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), qamdemodInLabels), 0, ("Qout", "Qin")),
      qamdemod2 ~> ps ## ("ch2_5", KernelPort(1, List((1, "0"), (1, "0"), (1, "1")), null),
        KernelPort(48, List((48, "10000")), psInLabels), 0, ("dout", "din2")),

      // Channel 2
      source3 ~> cp3 ## ("ch3_1_i", KernelPort(1, List((1, "1")), sourceOutLabels),
        KernelPort(80, List((80, "1"), (1, "0")), cpInLabels), 0, ("Iout", "Iin")),
      source3 ~> cp3 ## ("ch3_1_q", KernelPort(1, List((1, "1")), sourceOutLabels),
        KernelPort(80, List((80, "1"), (1, "0")), cpInLabels), 0, ("Qout", "Qin")),
      cp3 ~> fft3 ## ("ch3_2_i", KernelPort(64, List((17, "0"), (64, "1")), cpOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), fftInLabels), 0, ("Iout", "XSr")),
      cp3 ~> fft3 ## ("ch3_2_q", KernelPort(64, List((17, "0"), (64, "1")), cpOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), fftInLabels), 0, ("Qout", "XSi")),
      fft3 ~> zerocut3 ## ("ch3_3_i", KernelPort(64, List((64, "0"), (64, "1")), fftOutLabels),
        KernelPort(64, List((64, "1"), (1, "0")), zerocutInLabels), 0, ("XKr", "Iin")),
      fft3 ~> zerocut3 ## ("ch3_3_q", KernelPort(64, List((64, "0"), (64, "1")), fftOutLabels),
        KernelPort(64, List((64, "1"), (1, "0")), zerocutInLabels), 0, ("XKi", "Qin")),
      zerocut3 ~> qamdemod3 ## ("ch3_4_i", KernelPort(48, List((1, "0"), (48, "1"), (16, "0")), zerocutOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), qamdemodInLabels), 0, ("Iout", "Iin")),
      zerocut3 ~> qamdemod3 ## ("ch3_4_q", KernelPort(48, List((1, "0"), (48, "1"), (16, "0")), zerocutOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), qamdemodInLabels), 0, ("Qout", "Qin")),
      qamdemod3 ~> ps ## ("ch3_5", KernelPort(1, List((1, "0"), (1, "0"), (1, "1")), null),
        KernelPort(48, List((48, "10000")), psInLabels), 0, ("dout", "din3")),

      // Channel 3
      source4 ~> cp4 ## ("ch4_1_i", KernelPort(1, List((1, "1")), sourceOutLabels),
        KernelPort(80, List((80, "1"), (1, "0")), cpInLabels), 0, ("Iout", "Iin")),
      source4 ~> cp4 ## ("ch4_1_q", KernelPort(1, List((1, "1")), sourceOutLabels),
        KernelPort(80, List((80, "1"), (1, "0")), cpInLabels), 0, ("Qout", "Qin")),
      cp4 ~> fft4 ## ("ch4_2_i", KernelPort(64, List((17, "0"), (64, "1")), cpOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), fftInLabels), 0, ("Iout", "XSr")),
      cp4 ~> fft4 ## ("ch4_2_q", KernelPort(64, List((17, "0"), (64, "1")), cpOutLabels),
        KernelPort(64, List((64, "1"), (64, "0")), fftInLabels), 0, ("Qout", "XSi")),
      fft4 ~> zerocut4 ## ("ch4_3_i", KernelPort(64, List((64, "0"), (64, "1")), fftOutLabels),
        KernelPort(64, List((64, "1"), (1, "0")), zerocutInLabels), 0, ("XKr", "Iin")),
      fft4 ~> zerocut4 ## ("ch4_3_q", KernelPort(64, List((64, "0"), (64, "1")), fftOutLabels),
        KernelPort(64, List((64, "1"), (1, "0")), zerocutInLabels), 0, ("XKi", "Qin")),
      zerocut4 ~> qamdemod4 ## ("ch4_4_i", KernelPort(48, List((1, "0"), (48, "1"), (16, "0")), zerocutOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), qamdemodInLabels), 0, ("Iout", "Iin")),
      zerocut4 ~> qamdemod4 ## ("ch4_4_q", KernelPort(48, List((1, "0"), (48, "1"), (16, "0")), zerocutOutLabels),
        KernelPort(1, List((1, "0"), (1, "1"), (1, "0")), qamdemodInLabels), 0, ("Qout", "Qin")),
      qamdemod4 ~> ps ## ("ch4_5", KernelPort(1, List((1, "0"), (1, "0"), (1, "1")), null),
        KernelPort(48, List((48, "10000")), psInLabels), 0, ("dout", "din4")),


      ps ~> sink ## ("ch6", KernelPort(192, List((48, "01111")), null),
        KernelPort(1, List((1, "1")), null), 0, null))
  }

  /**
    * Rhino FM Digital Down Converter
    *
    * @return fmddc-graph
    */
  def fmddc: Graph[Actor, Channel] = {

    val sourceParams = new java.util.HashMap[String, Any]()
    //sourceParams.put("dWidth", 16)

    val ncoParams = new java.util.HashMap[String, Any]()
    ncoParams.put("FTW_WIDTH", 32)
    ncoParams.put("PHASE_WIDTH", 32)
    ncoParams.put("PHASE_DITHER_WIDTH", 22)

    val mixerParams = new java.util.HashMap[String, Any]()
    mixerParams.put("DIN1_WIDTH", 16)
    mixerParams.put("DIN2_WIDTH", 16)
    mixerParams.put("DOUT_WIDTH", 16)

    val cic1Params = new java.util.HashMap[String, Any]()
    cic1Params.put("DIN_WIDTH", 16)
    cic1Params.put("DOUT_WIDTH", 16)
    cic1Params.put("NUMBER_OF_STAGES", 10)
    cic1Params.put("DIFFERENTIAL_DELAY", 1)
    cic1Params.put("SAMPLE_RATE_CHANGE", 128)
    cic1Params.put("FILTER_TYPE", '0')
    cic1Params.put("CLKIN_PERIOD_NS", 0.0)

    val cfir1Params = new java.util.HashMap[String, Any]()
    cfir1Params.put("DIN_WIDTH", 16)
    cfir1Params.put("DOUT_WIDTH", 16)
    cfir1Params.put("NUMBER_OF_TAPS", 65)
    cfir1Params.put("FIR_LATENCY", 0)
    cfir1Params.put("COEFF_WIDTH", 16)
    cfir1Params.put("COEFFS", "coeff_type(27,33,26,5,-26,-57,-73,-56,0,82,155,177,115,-32,-217,-358,-367,-195,131," +
      "498,739,694,295,-382,-1110,-1562,-1423,-515,1118,3185,5216,6700,7244,6700,5216,3185,1118,-515,-1423,-1562," +
      "-1110,-382,295,694,739,498,131,-195,-367,-358,-217,-32,115,177,155,82,0,-56,-73,-57,-26,5,26,33,27)")

    val cic2Params = new java.util.HashMap[String, Any]()
    cic2Params.put("DIN_WIDTH", 16)
    cic2Params.put("DOUT_WIDTH", 16)
    cic2Params.put("NUMBER_OF_STAGES", 1)
    cic2Params.put("DIFFERENTIAL_DELAY", 1)
    cic2Params.put("SAMPLE_RATE_CHANGE", 4)
    cic2Params.put("FILTER_TYPE", '0')

    val cfir2Params = new java.util.HashMap[String, Any]()
    cfir2Params.put("DIN_WIDTH", 16)
    cfir2Params.put("DOUT_WIDTH", 16)
    cfir2Params.put("NUMBER_OF_TAPS", 65)
    cfir2Params.put("FIR_LATENCY", 0)
    cfir2Params.put("COEFF_WIDTH", 16)
    cfir2Params.put("COEFFS", "coeff_type(3,3,3,3,4,4,5,6,7,8,9,10,12,13,14,16,17,19,20,22,23,24,25,27,28,29,30,30," +
      "31,31,32,32,32,32,32,31,31,30,30,29,28,27,25,24,23,22,20,19,17,16,14,13,12,10,9,8,7,6,5,4,4,3,3,3,3)")

    val sinkParams = new java.util.HashMap[String, Any]()
    sinkParams.put("dWidth", 16)

    val (source, nco, mixeri, mixerq, cici1, cicq1, cfiri1, cfirq1, cici2, cicq2, cfiri2, cfirq2, sink) = (
      Actor("source", "sourceInst", sourceParams, 5, null),
      Actor("NCO", "ncoInst", ncoParams, 5, null),
      Actor("mixer", "mixeriInst", mixerParams, 2, null),
      Actor("mixer", "mixerqInst", mixerParams, 2, null),
      Actor("CIC", "cici1Inst", cic1Params, 128, null),
      Actor("CIC", "cicq1Inst", cic1Params, 128, null),
      Actor("fir_par", "cfiri1Inst", cfir1Params, 1, null),
      Actor("fir_par", "cfirq1Inst", cfir1Params, 1, null),
      Actor("CIC", "cici2Inst", cic2Params, 4, null),
      Actor("CIC", "cicq2Inst", cic2Params, 4, null),
      Actor("fir_par", "cfiri2Inst", cfir2Params, 1, null),
      Actor("fir_par", "cfirq2Inst", cfir2Params, 1, null),
      Actor("sink", "sinkInst", sinkParams, 1, null))


    //////////////////////////// LABELS  /////////////////////////////////


    val sourceOutLabels = new java.util.HashMap[String, String]()
    sourceOutLabels.put("dout", "dout")

    val ncoOutLabels = new java.util.HashMap[String, String]()
    ncoOutLabels.put("dout", "IOUT,QOUT")

    val mixerInLabels = new java.util.HashMap[String, String]()
    mixerInLabels.put("din", "din1,din2")
    val mixerOutLabels = new java.util.HashMap[String, String]()
    mixerOutLabels.put("dout", "dout")

    val sinkInLabels = new java.util.HashMap[String, String]()
    sinkInLabels.put("din", "Iin,Qin")


    Graph[Actor, Channel](
      source ~> mixeri ## ("ch1_i", KernelPort(1, List((4, "0"), (1, "1")), sourceOutLabels),
        KernelPort(1, List((1, "1"), (1, "0")), mixerInLabels), 0, ("dout", "din1")),
      source ~> mixerq ## ("ch1_q", KernelPort(1, List((4, "0"), (1, "1")), sourceOutLabels),
        KernelPort(1, List((1, "1"), (1, "0")), mixerInLabels), 0, ("dout", "din1")),
      nco ~> mixeri ## ("ch2_i", KernelPort(1, List((4, "0"), (1, "1")), ncoOutLabels),
        KernelPort(1, List((1, "1"), (1, "0")), mixerInLabels), 0, ("Iout", "din2")),
      nco ~> mixerq ## ("ch2_q", KernelPort(1, List((4, "0"), (1, "1")), ncoOutLabels),
        KernelPort(1, List((1, "1"), (1, "0")), mixerInLabels), 0, ("Qout", "din2")),
      mixeri ~> cici1 ## ("ch3_i", KernelPort(1, List((1, "0"), (1, "1")), null),
        KernelPort(128, List((128, "1")), null), 0, null),
      mixerq ~> cicq1 ## ("ch3_q", KernelPort(1, List((1, "0"), (1, "1")), null),
        KernelPort(128, List((128, "1")), null), 0, null),
      cici1 ~> cfiri1 ## ("ch4_i", KernelPort(1, List((127, "0"), (1, "1")), null),
        KernelPort(1, List((1, "1")), null), 0, null),
      cicq1 ~> cfirq1 ## ("ch4_q", KernelPort(1, List((127, "0"), (1, "1")), null),
        KernelPort(1, List((1, "1")), null), 0, null),
      cfiri1 ~> cici2 ## ("ch5_i", KernelPort(1, List((1, "1")), null),
        KernelPort(4, List((4, "1")), null), 0, null),
      cfirq1 ~> cicq2 ## ("ch5_q", KernelPort(1, List((1, "1")), null),
        KernelPort(4, List((4, "1")), null), 0, null),
      cici2 ~> cfiri2 ## ("ch6_i", KernelPort(1, List((3, "0"), (1, "1")), null),
        KernelPort(1, List((1, "1")), null), 0, (null)),
      cicq2 ~> cfirq2 ## ("ch6_q", KernelPort(1, List((3, "0"), (1, "1")), null),
        KernelPort(1, List((1, "1")), null), 0, null),
      cfiri2 ~> sink ## ("ch7_i", KernelPort(1, List((1, "1")), null),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("dout", "Iin")),
      cfirq2 ~> sink ## ("ch7_q", KernelPort(1, List((1, "1")), null),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("dout", "Qin")))
  }

  /**
    * Rhino GSM Digital Down Converter
    *
    * @return gsmddc-graph
    */
  def gsmddc: Graph[Actor, Channel] = {

    val sourceParams = new java.util.HashMap[String, Any]()
    //sourceParams.put("dWidth", 16)

    val ncoParams = new java.util.HashMap[String, Any]()
    ncoParams.put("FTW_WIDTH", 32)
    ncoParams.put("PHASE_WIDTH", 32)
    ncoParams.put("PHASE_DITHER_WIDTH", 22)

    val mixerParams = new java.util.HashMap[String, Any]()
    mixerParams.put("DIN1_WIDTH", 16)
    mixerParams.put("DIN2_WIDTH", 16)
    mixerParams.put("DOUT_WIDTH", 16)

    val cic1Params = new java.util.HashMap[String, Any]()
    cic1Params.put("DIN_WIDTH", 16)
    cic1Params.put("DOUT_WIDTH", 16)
    cic1Params.put("NUMBER_OF_STAGES", 10)
    cic1Params.put("DIFFERENTIAL_DELAY", 1)
    cic1Params.put("SAMPLE_RATE_CHANGE", 128)
    cic1Params.put("FILTER_TYPE", '0')
    cic1Params.put("CLKIN_PERIOD_NS", 0.0)

    val cfir1Params = new java.util.HashMap[String, Any]()
    cfir1Params.put("DIN_WIDTH", 16)
    cfir1Params.put("DOUT_WIDTH", 16)
    cfir1Params.put("NUMBER_OF_TAPS", 30)
    cfir1Params.put("FIR_LATENCY", 0)
    cfir1Params.put("COEFF_WIDTH", 16)
    cfir1Params.put("COEFFS", "coeff_type(27,33,26,5,-26,-57,-73,-56,0,82,155,177,115,-32,-217,-358,-367,-195,131," +
      "498,739,694,295,-382,-1110,-1562,-1423,-515,1118,3185)")


    val cfir2Params = new java.util.HashMap[String, Any]()
    cfir2Params.put("DIN_WIDTH", 16)
    cfir2Params.put("DOUT_WIDTH", 16)
    cfir2Params.put("NUMBER_OF_TAPS", 35)
    cfir2Params.put("FIR_LATENCY", 0)
    cfir2Params.put("COEFF_WIDTH", 16)
    cfir2Params.put("COEFFS", "coeff_type(3,3,3,3,4,4,5,6,7,8,9,10,12,13,14,16,17,19,20,22,23,24,25,27,28,29,30,30," +
      "31,31,32,32,32,32,32)")

    val sinkParams = new java.util.HashMap[String, Any]()
    sinkParams.put("dWidth", 16)

    val (source, nco, mixeri, mixerq, cici, cicq, cfiri, cfirq, pfiri, pfirq, sink) = (
      Actor("source", "sourceInst", null, 5, null),
      Actor("NCO", "ncoInst", ncoParams, 5, null),
      Actor("mixer", "mixeriInst", mixerParams, 2, null),
      Actor("mixer", "mixerqInst", mixerParams, 2, null),
      Actor("CIC", "cici1Inst", cic1Params, 256, null),
      Actor("CIC", "cicq1Inst", cic1Params, 256, null),
      Actor("fir_par", "cfiri1Inst", cfir1Params, 1, null),
      Actor("fir_par", "cfirq1Inst", cfir1Params, 1, null),
      Actor("fir_par", "pfiriInst", cfir2Params, 1, null),
      Actor("fir_par", "pfirqInst", cfir2Params, 1, null),
      Actor("sink", "sinkInst", sinkParams, 1, null))

    //////////////////////////// LABELS  /////////////////////////////////

    /*val sourceInLabels = new java.util.HashMap[String, String]()
    sourceInLabels.put("din", "Iin,Qin")*/
    val sourceOutLabels = new java.util.HashMap[String, String]()
    sourceOutLabels.put("dout", "dout")

    val ncoOutLabels = new java.util.HashMap[String, String]()
    ncoOutLabels.put("dout", "IOUT,QOUT")

    val mixerInLabels = new java.util.HashMap[String, String]()
    mixerInLabels.put("din", "din1,din2")
    val mixerOutLabels = new java.util.HashMap[String, String]()
    mixerOutLabels.put("dout", "dout")

    /* val cic1InLabels = new java.util.HashMap[String, String]()
     cic1InLabels.put("din", "Iin,Qin")
     val cic1OutLabels = new java.util.HashMap[String, String]()
     cic1OutLabels.put("dout", "Iout,Qout")

     val cfir1InLabels = new java.util.HashMap[String, String]()
     cfir1InLabels.put("din", "Iin,Qin")
     val cfir1OutLabels = new java.util.HashMap[String, String]()
     cfir1OutLabels.put("dout", "Iout,Qout")

     val cic2InLabels = new java.util.HashMap[String, String]()
     cic2InLabels.put("din", "Iin,Qin")
     val cic2OutLabels = new java.util.HashMap[String, String]()
     cic2OutLabels.put("dout", "Iout,Qout")

     val cfir2InLabels = new java.util.HashMap[String, String]()
     cfir2InLabels.put("din", "Iin,Qin")
     val cfir2OutLabels = new java.util.HashMap[String, String]()
     cfir2OutLabels.put("dout", "Iout,Qout")    */

    val sinkInLabels = new java.util.HashMap[String, String]()
    sinkInLabels.put("din", "Iin,Qin")


    Graph[Actor, Channel](
      source ~> mixeri ## ("ch1_i", KernelPort(1, List((4, "0"), (1, "1")), sourceOutLabels),
        KernelPort(1, List((1, "1"), (1, "0")), mixerInLabels), 0, ("dout", "din1")),
      source ~> mixerq ## ("ch1_q", KernelPort(1, List((4, "0"), (1, "1")), sourceOutLabels),
        KernelPort(1, List((1, "1"), (1, "0")), mixerInLabels), 0, ("dout", "din1")),
      nco ~> mixeri ## ("ch2_i", KernelPort(1, List((4, "0"), (1, "1")), ncoOutLabels),
        KernelPort(1, List((1, "1"), (1, "0")), mixerInLabels), 0, ("Qout", "din2")),
      nco ~> mixerq ## ("ch2_q", KernelPort(1, List((4, "0"), (1, "1")), ncoOutLabels),
        KernelPort(1, List((1, "1"), (1, "0")), mixerInLabels), 0, ("Qout", "din2")),
      mixeri ~> cici ## ("ch3_i", KernelPort(1, List((1, "0"), (1, "1")), null),
        KernelPort(256, List((256, "1")), null), 0, null),
      mixerq ~> cicq ## ("ch3_q", KernelPort(1, List((1, "0"), (1, "1")), null),
        KernelPort(256, List((256, "1")), null), 0, null),
      cici ~> cfiri ## ("ch4_i", KernelPort(1, List((255, "0"), (1, "1")), null),
        KernelPort(1, List((1, "1")), null), 0, null),
      cicq ~> cfirq ## ("ch4_q", KernelPort(1, List((255, "0"), (1, "1")), null),
        KernelPort(1, List((1, "1")), null), 0, null),
      cfiri ~> pfiri ## ("ch5_i", KernelPort(1, List((1, "1")), null),
        KernelPort(1, List((1, "1")), null), 0, null),
      cfirq ~> pfirq ## ("ch5_q", KernelPort(1, List((1, "1")), null),
        KernelPort(1, List((1, "1"), (1, "0")), null), 0, null),
      pfiri ~> sink ## ("ch6_i", KernelPort(1, List((1, "1")), null),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("dout", "Iin")),
      pfirq ~> sink ## ("ch6_q", KernelPort(1, List((1, "1")), null),
        KernelPort(1, List((1, "1")), sinkInLabels), 0, ("dout", "Qin")))
  }

  /**
    * SDF-AP Sample Application
    *
    * @return sdapsample-graph
    */
  def sdapsample: Graph[Actor, Channel] = {
    val xParams = new java.util.HashMap[String, Any]()
    xParams.put("dWidth", 8)
    val yParams = new java.util.HashMap[String, Any]()
    yParams.put("dWidth", 8)

    val (x, y) = (Actor("x", "xInst", xParams, 3, null),
      Actor("y", "yInst", yParams, 5, null))

    val ch1 = Channel[Actor](x, y, "xInst_yInst_ch1",
      KernelPort(2, List((1, "011")), null),
      KernelPort(3, List((1, "10101")), null), 0, null)

    Graph[Actor, Channel](ch1)
  }

}