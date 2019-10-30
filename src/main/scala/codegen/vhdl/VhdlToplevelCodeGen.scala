package sdrlift.codegen.vhdl

// vMagic
import de.upb.hni.vmagic.VhdlFile
import de.upb.hni.vmagic.builtin.StdLogic1164
import de.upb.hni.vmagic.concurrent.ConditionalSignalAssignment
import de.upb.hni.vmagic.concurrent.ProcessStatement
import de.upb.hni.vmagic.declaration.Attribute
import de.upb.hni.vmagic.declaration.SignalDeclaration
import de.upb.hni.vmagic.expression.Add
import de.upb.hni.vmagic.expression.And
import de.upb.hni.vmagic.expression.Equals
import de.upb.hni.vmagic.libraryunit.Architecture
import de.upb.hni.vmagic.libraryunit.Entity
import de.upb.hni.vmagic.libraryunit.LibraryClause
import de.upb.hni.vmagic.libraryunit.UseClause
import de.upb.hni.vmagic.`object`.AttributeExpression
import de.upb.hni.vmagic.`object`.Signal
import de.upb.hni.vmagic.`object`.Constant
import de.upb.hni.vmagic.statement.IfStatement
import de.upb.hni.vmagic.statement.SignalAssignment
import de.upb.hni.vmagic._
import de.upb.hni.vmagic.`object`.VhdlObject.Mode
import de.upb.hni.vmagic.builtin.Standard
import de.upb.hni.vmagic.output.VhdlOutput
import sdrlift.model.Channel.FifoProps


//Graph
import scalax.collection.GraphPredef._
import scalax.collection.GraphEdge._
import scalax.collection.io.dot._
import scalax.collection.io.dot.implicits._

import scalax.collection.Graph
import sdrlift.model.{Actor, Channel}
import sdrlift.model.Sdfap._

import java.io.PrintWriter
import java.io._
import sys.process._
import scala.language.postfixOps
import scala.collection.JavaConversions._
import de.upb.hni.vmagic.expression.Aggregate
import de.upb.hni.vmagic.statement.CaseStatement
import de.upb.hni.vmagic.literal.BinaryLiteral
import de.upb.hni.vmagic.literal.DecimalLiteral
import de.upb.hni.vmagic.literal.BasedLiteral
import de.upb.hni.vmagic.`type`.IndexSubtypeIndication
import de.upb.hni.vmagic.declaration.ConstantDeclaration

case class VhdlToplevelCodeGen(graph: Graph[Actor, Channel]){

  /**
    * Get the output data register width of an actor
    */
  def getOutDataWidth(signals: List[Signal], label: String): Int = {
    val signal = signals.find(_.getIdentifier.equalsIgnoreCase(label)).get

    if (signal.getType.isInstanceOf[IndexSubtypeIndication])
      signal.getType.asInstanceOf[IndexSubtypeIndication].getRanges.get(0).asInstanceOf[Range].getFrom.toString.toInt + 1
    else
      signal.getPrecedence
  }

  /**
    * Case insensitive value search using a map key. It also ignore leading
    */
  def getMapValByKey(m: scala.collection.mutable.Map[String, String], key: String) = {
    val mt = m map { case (k, v) => k.toLowerCase.replaceAll("""(?m)\s+$""", "") -> v }
    val kt = key.toLowerCase.replaceAll("""(?m)\s+$""", "")
    if (mt.find(_._1.equalsIgnoreCase(kt)) != None) mt.find(_._1.equalsIgnoreCase(kt)).get._2 else null
  }

  /**
    * Get all the ports for each actor
    */
  def getPorts(): Map[Actor, java.util.HashMap[String, String]] = {


    def mergeMap(list: List[java.util.HashMap[String, String]]): java.util.HashMap[String, String] = {
      var labels = new java.util.HashMap[String, String]()
      for (i <- list) {
        for (j <- i) labels.put(j._1, j._2)
      }
      labels
    }

    val list = graph.nodes map { n =>
      (n, n.incoming.map(_.snkPort).toList ::: n.outgoing.map(_.srcPort).toList)
    }

    var ports = Map[Actor, java.util.HashMap[String, String]]()
    for (i <- list) {
      val l = i._2 map (e => e.labels)
      val m = mergeMap(l.filter(_ != null))
      val im = m.flatMap { s => s._2.split(",").map { t => t -> s._1 }.toMap }

      var labels = new java.util.HashMap[String, String]()

      def getSignalMode(s: Signal) = if (s.getMode == Mode.IN) "_i" else "_o"

      i._1.signals foreach { s =>
        val str = s.getIdentifier

        if (str.equalsIgnoreCase("en")) labels.put(i._1.inst + "_" + str + getSignalMode(s), str)
        else if (str.equalsIgnoreCase("din")) labels.put(i._1.inst + "_" + str + getSignalMode(s), str)
        else if (str.equalsIgnoreCase("vld")) labels.put(i._1.inst + "_" + str + getSignalMode(s), str)
        else if (str.equalsIgnoreCase("dout")) labels.put(i._1.inst + "_" + str + getSignalMode(s), str)
        else {
          if (getMapValByKey(im, str) != null) {
            labels.put(i._1.inst + "_" + s.getIdentifier + getSignalMode(s), im.getOrElse(s.getIdentifier, s
              .getIdentifier))
          }

        }
      }
      ports += Actor(i._1.id, i._1.inst, i._1.params, i._1.execTime) -> labels
    }

    ports
  }

  /*

  /**
    * Generate the HDL of the top level system
    */
  def toplevel(slots: Map[String, FifoProps], name: String, libClauses: List[String], useClauses: List[String],
               ver: Int): (VhdlFile, Int) = {

    val file: VhdlFile = new VhdlFile
    val entity: Entity = new Entity(name)
    val architecture: Architecture = new Architecture("rtl", entity);
    var totalFsms = 0

    /**
      * creates the VHDL file and adds Designunits to it
      *
      * @param wordWidth the width of the counter
      * @return A VhdlFile containing the implementation
      */
    def createFile: VhdlFile = {
      //file = new VhdlFile();
      // Add Elements
      file.getElements.add(new LibraryClause("IEEE"))
      file.getElements.add(StdLogic1164.USE_CLAUSE)
      file.getElements.add(new UseClause("ieee.std_logic_unsigned.all"))

      if (libClauses != null) {
        for (libClause <- libClauses)
          file.getElements.add(new LibraryClause(libClause))
      }

      if (useClauses != null) {
        for (useClause <- useClauses)
          file.getElements.add(new UseClause(useClause))
      }

      file.getElements.add(entity)
      file.getElements.add(createArchitecture)
      file
    }

    /**
      * Create the entity of the counter,
      * all elements but the output are global, output is
      * generated here according to width
      *
      * @return the entity
      */
    def createEntity: Entity = {
      // entity.getPort.add(clk)
      entity
    }

    /**
      * retrieve the postfix mode of the signal
      */
    def getSignalMode(s: Signal) = if (s.getMode == Mode.IN) "_i" else "_o"

    /**
      * Get the width of the signal
      */
    def getEntitySignalType(a: Actor, s: Signal) = {
      if (s.getType == StdLogic1164.STD_LOGIC)
        s.getType
      else {
        val fifoWidth = getOutDataWidth(a.wires, a.inst + "_" + s.getIdentifier + getSignalMode(s))
        StdLogic1164.STD_LOGIC_VECTOR(fifoWidth)
      }
    }

    def condSignalAssignment(dstSignal: Signal, srcSignal: Signal) = {
      val dstSignalMSBIndex = dstSignal.getType.asInstanceOf[IndexSubtypeIndication].getRanges.get(0)
        .asInstanceOf[Range].getFrom.toString.toInt
      val srcSignalMSBIndex = srcSignal.getType.asInstanceOf[IndexSubtypeIndication].getRanges.get(0)
        .asInstanceOf[Range].getFrom.toString.toInt

      if (dstSignalMSBIndex == srcSignalMSBIndex)
        new ConditionalSignalAssignment(dstSignal, srcSignal)
      else if (dstSignalMSBIndex < srcSignalMSBIndex)
        new ConditionalSignalAssignment(dstSignal, srcSignal.
          getSlice(new Range(srcSignalMSBIndex, Range.Direction.DOWNTO, srcSignalMSBIndex - dstSignalMSBIndex)))
      else
        null
    }

    /**
      * Implement the counter, declare internal signals etc.
      *
      * @return the architecture containing the xounter implementation
      */
    def createArchitecture: Architecture = {

      // Firing Repetition
      val RV = graph.repetition
      // common edges indices
      val CI = graph.commonEdgesIndices
      // common edges indices
      val CC = graph.commonEdgesCount

      // root and end actors
      val rootActors = graph.getRootActors
      val endActors = graph.getEndActors

      // get ports
      val ports = getPorts

      var countCC = 0

      var declaredActors: List[String] = List()
      var declaredChannels: List[String] = List()


      // get bit type
      def bitVal(b: Int) = if (b == 0) StdLogic1164.STD_LOGIC_0 else StdLogic1164.STD_LOGIC_1
      //var prevSnkSchedulingPeriod = 0

      def getChannelPortMap(p: (String, String)): (String, String) = {
        if (p != null) p else (null, null)
      }

      // generation of global register signals
      val clk: Signal = new Signal("clk", Mode.IN, StdLogic1164.STD_LOGIC)
      val rst: Signal = new Signal("rst", Mode.IN, StdLogic1164.STD_LOGIC)
      entity.getPort.add(clk)
      entity.getPort.add(rst)

      var fsmLength = 0
      var lastChannelPortMap: (String, String) = null
      var srcSchedulePeriod: Int = 0
      var topInputSignalIdentifier_i = 0
      var topOutputSignalIdentifier_i = 0

      // traverse the actors
      graph.getOrderedActors /*g.nodes*/ map { n =>

        /** root actor operations */
        val filteredTopInputSignals = Set("clk", "rst")
        if (rootActors.contains(n)) {
          n.component.getPort.flatMap(e => e.getVhdlObjects).filter(_.getMode == Mode.IN)
            .foreach { s =>

              /** extract input ports from the root actor and add them to toplevel entity */
              if (!filteredTopInputSignals.contains(s.getIdentifier))
                entity.getPort.add(new Signal(s.getIdentifier + topInputSignalIdentifier_i, s.getMode,
                  getEntitySignalType(n, s)))

              /** register assignment ralating to root actor */
              if (!filteredTopInputSignals.contains(s.getIdentifier))
                architecture.getStatements.add(new ConditionalSignalAssignment(new Signal(n.inst + "_" + s
                  .getIdentifier + getSignalMode(s), StdLogic1164.STD_LOGIC_VECTOR(0)), new Signal(s.getIdentifier +
                  topInputSignalIdentifier_i, Standard.INTEGER, new DecimalLiteral(0))))
            }
          topInputSignalIdentifier_i = topInputSignalIdentifier_i + 1
        }

        /** declare the actor components */
        if (!declaredActors.contains(n.id)) {
          architecture.getDeclarations.add(n.component)
          declaredActors = declaredActors ::: List(n.id)
        }

        /** declare actor component register signals */
        n.wires map { s => architecture.getDeclarations.add(new SignalDeclaration(s)) }
        /** instantiate a component */
        val compInst = n.instance(n.wires)
        architecture.getStatements.add(compInst)


        /** map the incoming channel of each actor */
        graph.incomingChannels(n).map { e =>

          val buffer = slots.get(e.id).get

          /** declare the channel */
          if (!declaredChannels.contains("fifo")) {
            architecture.getDeclarations.add(e.component)
            declaredChannels = declaredChannels ::: List("fifo")
          }
          /** declare fifo component register signals and constants */
          val fifoWidth = getOutDataWidth(
            e.source.wires, e.source.wire("dout", ports.get(e.source).get, e.source.inst + "_" + getChannelPortMap(e
              .portMap)._1 + "_o").getIdentifier)

          if (!(buffer.size == 1 && (ver == 3 || ver == 4)))
            e.wires(fifoWidth) map { s => architecture.getDeclarations.add(new SignalDeclaration(s)) }

          /** instantiate a fifo component */
          val channelParams = new java.util.HashMap[String, Any]()
          channelParams.put("DATA_WIDTH", fifoWidth)
          channelParams.put("FIFO_DEPTH", buffer.size)

          if (!(buffer.size == 1 && (ver == 3 || ver == 4)))
            architecture.getStatements.add(e.instance(channelParams, fifoWidth))

          /** Sequential Control logic signals */
          if (!(buffer.size == 1 && (ver == 3 || ver == 4))) {
            architecture.getStatements.add(
              new ConditionalSignalAssignment(e.wire("we"), e.source.wire("vld", ports.get(e.source).get, null)))

            architecture.getStatements.add(
              new ConditionalSignalAssignment(e.wire("din"), e.source.wire("dout", ports.get(e.source).get,
                e.source.inst + "_" + getChannelPortMap(e.portMap)._1 + "_o")))

            if (CI.get(e).get == 0) architecture.getStatements.add(new ConditionalSignalAssignment(e.target.wire
            ("en", ports.get(e.target).get, null), e.wire("vld")))
            architecture.getStatements.add(new ConditionalSignalAssignment(e.target.wire("din", ports.get(e.target)
              .get, e.target.inst + "_" + getChannelPortMap(e.portMap)._2 + "_i"), e.wire("dout")))
          }

          /** declare a process */
          val process: ProcessStatement = new ProcessStatement(e.id + "_proc")


          // add a process reset IfStatement
          val vProcessIf: IfStatement = new IfStatement(new Equals(rst, StdLogic1164.STD_LOGIC_1))
          // reset process registers inside the reset IfStatement

          if (!(buffer.size == 1 && (ver == 3 || ver == 4)))
            vProcessIf.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
          else {
            if (CI.get(e).get == 0) vProcessIf.getStatements.
              add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null), StdLogic1164.STD_LOGIC_0))
          }

          // add the IfStatement for rising edge of the clock
          vProcessIf.createElsifPart(
            new And(new AttributeExpression[Signal](clk, new Attribute("EVENT", null)),
              new Equals(clk, StdLogic1164.STD_LOGIC_1)))
          if (!(buffer.size == 1 && (ver == 3 || ver == 4)))
            vProcessIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(e.wire("re"),
              StdLogic1164.STD_LOGIC_0))

          // add states

          ver match {

            // Version 0 implements all states wich many case statements
            case 0 => {

              fsmLength = buffer.snkStartSchedule + buffer.ii - 2 // corresponds to read signal in state
              val fsmLog2 = Math.ceil(Math.log10(fsmLength) / Math.log10(2)).toInt
              val fsmZeros = Array.fill(fsmLog2)(0).mkString

              // Signal Declaration
              val stateSig = new Signal(e.id + "_state", StdLogic1164.STD_LOGIC_VECTOR(fsmLog2), Aggregate.OTHERS
              (StdLogic1164.STD_LOGIC_0))
              architecture.getDeclarations.add(new SignalDeclaration(stateSig))

              // Reset signals in process
              vProcessIf.getStatements.add(new SignalAssignment(stateSig, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
              vProcessIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral
              (fsmZeros)))

              // version 0 add states
              val FSM = new CaseStatement(stateSig)
              val fsms = for (i <- 0 until fsmLength)
                yield FSM.createAlternative(new BinaryLiteral(i.toBinaryString.reverse.padTo(fsmLog2, "0").
                  reverse.mkString))

              val stateX = FSM.createAlternative(new BasedLiteral("others"))
              stateX.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(0)))
              stateX.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral(fsmZeros)))

              // Defining States
              var stateCount = 0
              for (state <- fsms) {
                if (stateCount == 0) { // First State

                  state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral(fsmZeros)))
                  // add this IfStatement to the state 0
                  val vState0If: IfStatement = new IfStatement(new Equals(e.source.wire("vld", ports.get(e.source)
                    .get, null), StdLogic1164.STD_LOGIC_1))
                  vState0If.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(buffer.snkReadEnCtrl
                  (stateCount + 2))))
                  vState0If.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 2)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vState0If);

                } else if (stateCount == fsmLength - 1) { // Last State
                  state.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(buffer.snkReadEnCtrl(stateCount))))
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((if (buffer
                    .snkStartSchedule - 2 <= 0) 0 else buffer.snkStartSchedule - 2).toBinaryString.reverse.padTo
                  (fsmLog2, "0").reverse.mkString))) // if(buffer.snkStartSchedule == 0) 0 else

                } else { // Intermediate States

                  state.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(buffer.snkReadEnCtrl(stateCount +
                    1))))
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))

                }
                stateCount = stateCount + 1
              }

              vProcessIf.getElsifParts.get(0).getStatements().add(FSM);

            }

            case 1 => {

              //  val snkLastStepSize = buffer.ii - (buffer.snkSchedulingPeriod * RV.get(e.target).get) + buffer
              // .snkSchedulingPeriod

              val iterationGap = buffer.ii - ((buffer.snkSchedulingPeriod * (RV.get(e.target).get - 1)) + e.target
                .execTime) // TODO: Update in algorithm
              val firingGap = buffer.snkSchedulingPeriod - e.target.execTime
              val startupGap = buffer.snkStartSchedule - buffer.srcAp.indexOf(1) - 1
              val loopStartIndex = if (startupGap > 1) 2 else 1

              /*println ("loopStartIndex: " + loopStartIndex)
              println ("startupGap: " + startupGap)
              println ("firingGap: " + firingGap)
              println ("iterationGap: " + iterationGap)*/

              fsmLength = e.target.execTime + loopStartIndex + (if (firingGap > 0) 1 else 0) + (if (iterationGap > 0)
                1 else 0) // corresponds to read signal in state

              val fsmLog2 = Math.ceil(Math.log10(fsmLength) / Math.log10(2)).toInt
              val fsmZeros = Array.fill(fsmLog2)(0).mkString

              // Signal Declaration

              val firingGapSig = new Signal(e.id + "_fgc", Standard.INTEGER, new DecimalLiteral(0))
              val icSig = new Signal(e.id + "_ic", Standard.INTEGER, new DecimalLiteral(0))
              if (firingGap > 0)
                architecture.getDeclarations.add(new SignalDeclaration(icSig))
              if (firingGap > 0)
                architecture.getDeclarations.add(new SignalDeclaration(firingGapSig))
              val iterationGapSig = new Signal(e.id + "_igc", Standard.INTEGER, new DecimalLiteral(0))
              val jcSig = new Signal(e.id + "_jc", Standard.INTEGER, new DecimalLiteral(0))
              if (iterationGap > 0)
                architecture.getDeclarations.add(new SignalDeclaration(iterationGapSig))
              if (iterationGap > 0)
                architecture.getDeclarations.add(new SignalDeclaration(jcSig))
              val startupGapSig = new Signal(e.id + "_sgc", Standard.INTEGER, new DecimalLiteral(0))
              if (startupGap > 1)
                architecture.getDeclarations.add(new SignalDeclaration(startupGapSig))
              val stateSig = new Signal(e.id + "_state", StdLogic1164.STD_LOGIC_VECTOR(fsmLog2), Aggregate.OTHERS
              (StdLogic1164.STD_LOGIC_0))
              architecture.getDeclarations.add(new SignalDeclaration(stateSig))

              // Reset signals in process
              vProcessIf.getStatements.add(new SignalAssignment(stateSig, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
              if (firingGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(firingGapSig, new DecimalLiteral(0)))
              if (firingGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(icSig, new DecimalLiteral(0)))
              if (iterationGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(iterationGapSig, new DecimalLiteral(0)))
              if (iterationGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(jcSig, new DecimalLiteral(0)))
              if (startupGap > 1) vProcessIf.getStatements.add(new SignalAssignment(startupGapSig, new DecimalLiteral
              (0)))
              vProcessIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral
              (fsmZeros)))

              // State
              val FSM = new CaseStatement(stateSig)
              val fsms = for (i <- 0 until fsmLength)
                yield FSM.createAlternative(new BinaryLiteral(i.toBinaryString.reverse.padTo(fsmLog2, "0").reverse
                  .mkString))

              val stateX = FSM.createAlternative(new BasedLiteral("others"))
              stateX.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(0)))
              stateX.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral(fsmZeros)))

              // Defining States
              var firingGapExists = false
              var iterationGapExists = false
              var startupGapExists = false
              var stateCount = 0
              val apCountStart = if (startupGap > 1) 0 else buffer.snkAp.indexOf(1)
              var apCount = apCountStart

              for (state <- fsms) {
                if (stateCount == 0) { // First State

                  state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral(fsmZeros)))
                  // add this IfStatement to the state 0

                  val vState0If: IfStatement = new IfStatement(new Equals(e.source.wire("vld", ports.get(e.source)
                    .get, null), StdLogic1164.STD_LOGIC_1))
                  if (startupGap > 1) {
                    vState0If.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                    if (startupGap > 1)
                      vState0If.getStatements.add(new SignalAssignment(startupGapSig, new Add(startupGapSig, new
                          DecimalLiteral(1))))
                    vState0If.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  } else if (e.target.execTime == 1) {
                    vState0If.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(buffer.snkAp(apCount))))
                    vState0If.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  } else {
                    vState0If.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(buffer.snkAp(apCount))))
                    vState0If.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((loopStartIndex +
                      apCountStart + 1).toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  }

                  state.getStatements.add(vState0If);

                } else if (startupGap > 1 && startupGapExists == false) {

                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((1).toBinaryString.reverse
                    .padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(startupGapSig, new Add(startupGapSig, new
                      DecimalLiteral(1))))
                  state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  val vState0If = new IfStatement(new Equals(startupGapSig, new DecimalLiteral(startupGap - 1)))
                  vState0If.getStatements.add(new SignalAssignment(startupGapSig, new DecimalLiteral(0)))
                  // vState0If.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(readEnVec(apCount))))
                  vState0If.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vState0If)
                  // apCount = apCount + 1
                  startupGapExists = true

                } /*else if(startupGap > 1 && stateCount == 1 && startupGapExists == false){

                   state.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(buffer.snkReadEnCtrl
                   (startupOffset + stateCount))))
                   state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount+1)
                   .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString))) // if(buffer.snkStartSchedule ==
                   0) 0 else

                 }*/ else if (apCount == (e.target.execTime + apCountStart) - 1) { // The Last State Before Any Delays

                  state.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(buffer.snkAp(apCount -
                    apCountStart))))

                  if (firingGap > 0 && iterationGap > 0) {

                    state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))

                    state.getStatements.add(new SignalAssignment(jcSig, new Add(jcSig, new DecimalLiteral(1))))
                    val vStateNIf1 = new IfStatement(new Equals(jcSig, new DecimalLiteral(RV.get(e.target).get - 1)))
                    vStateNIf1.getStatements.add(new SignalAssignment(jcSig, new DecimalLiteral(0)))
                    vStateNIf1.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 2)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                    state.getStatements.add(vStateNIf1);

                    firingGapExists = true
                    iterationGapExists = true

                  } else if (firingGap > 0) {

                    state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                    firingGapExists = true

                  } else if (iterationGap > 0) {

                    state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((loopStartIndex)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                    state.getStatements.add(new SignalAssignment(jcSig, new Add(jcSig, new DecimalLiteral(1))))
                    val vStateNIf = new IfStatement(new Equals(jcSig, new DecimalLiteral(RV.get(e.target).get - 1)))
                    vStateNIf.getStatements.add(new SignalAssignment(jcSig, new DecimalLiteral(0)))
                    vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                    state.getStatements.add(vStateNIf)
                    iterationGapExists = true

                  } else {

                    state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))

                  }

                  apCount = apCount + 1
                } else if ((stateCount == fsmLength - 1 - (if (iterationGap > 0) 1 else 0)) && firingGapExists ==
                  true) {

                  state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(firingGapSig, new Add(firingGapSig, new DecimalLiteral
                  (1))))
                  val vStateNIf = new IfStatement(new Equals(firingGapSig, new DecimalLiteral(firingGap - 1)))
                  vStateNIf.getStatements.add(new SignalAssignment(firingGapSig, new DecimalLiteral(0)))
                  vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((loopStartIndex)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vStateNIf);

                } else if (iterationGapExists == true) {

                  state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(iterationGapSig, new Add(iterationGapSig, new
                      DecimalLiteral(1))))
                  val vStateNIf = new IfStatement(new Equals(iterationGapSig, new DecimalLiteral(iterationGap - 1)))
                  vStateNIf.getStatements.add(new SignalAssignment(iterationGapSig, new DecimalLiteral(0)))
                  vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((loopStartIndex)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vStateNIf);

                } else if (apCount < (e.target.execTime + apCountStart)) { // Intermediate States

                  state.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(buffer.snkAp(apCount -
                    apCountStart))))
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))

                  apCount = apCount + 1

                } else { // last state before condition = if(apCount < e.target.execTime)

                  state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString))) // if(buffer.snkStartSchedule ==
                  // 0) 0 else

                }
                stateCount = stateCount + 1
              }

              vProcessIf.getElsifParts.get(0).getStatements().add(FSM);

            }

            case 2 => {

              // val snkLastStepSize = buffer.ii - (buffer.snkSchedulingPeriod * RV.get(e.target).get) + buffer
              // .snkSchedulingPeriod

              val iterationGap = buffer.ii - ((buffer.snkSchedulingPeriod * (RV.get(e.target).get - 1)) + e.target
                .execTime) // TODO: Update in algorithm
              val firingGap = buffer.snkSchedulingPeriod - e.target.execTime
              val startupGap = buffer.snkStartSchedule - buffer.srcAp.indexOf(1) - 1 // minus 1 for ReadEn
              val loopStartIndex = if (startupGap > 1) 2 else 1

              fsmLength = (if (startupGap > 1) 1 else 0) + (if (firingGap > 0) 1 else 0) + (if (iterationGap > 0) 1
              else 0) + 2 // corresponds to read signal in state

              val fsmLog2 = Math.ceil(Math.log10(fsmLength) / Math.log10(2)).toInt
              val fsmZeros = Array.fill(fsmLog2)(0).mkString

              // Signal Declaration

              val firingGapSig = new Signal(e.id + "_fgc", Standard.INTEGER, new DecimalLiteral(0))
              if (firingGap > 0)
                architecture.getDeclarations.add(new SignalDeclaration(firingGapSig))
              val icSig = new Signal(e.id + "_ic", Standard.INTEGER, new DecimalLiteral(0))
              architecture.getDeclarations.add(new SignalDeclaration(icSig))
              val iterationGapSig = new Signal(e.id + "_igc", Standard.INTEGER, new DecimalLiteral(0))
              val jcSig = new Signal(e.id + "_jc", Standard.INTEGER, new DecimalLiteral(0))
              if (iterationGap > 0)
                architecture.getDeclarations.add(new SignalDeclaration(iterationGapSig))
              if (iterationGap > 0 && firingGap > 0) architecture.getDeclarations.add(new SignalDeclaration(jcSig))
              val startupGapSig = new Signal(e.id + "_sgc", Standard.INTEGER, new DecimalLiteral(0))
              if (startupGap > 1)
                architecture.getDeclarations.add(new SignalDeclaration(startupGapSig))
              val stateSig = new Signal(e.id + "_state", StdLogic1164.STD_LOGIC_VECTOR(fsmLog2), Aggregate.OTHERS
              (StdLogic1164.STD_LOGIC_0))
              architecture.getDeclarations.add(new SignalDeclaration(stateSig))

              val readEnVec = buffer.snkAp.mkString //buffer.snkReadEnCtrl.slice(buffer.snkStartSchedule - 1, buffer
              // .snkStartSchedule + e.target.execTime).mkString
              val readEnConst = new Constant(e.id + "_cp", StdLogic1164.STD_LOGIC_VECTOR(readEnVec.length), new
                  BinaryLiteral(readEnVec.reverse))
              architecture.getDeclarations.add(new ConstantDeclaration(readEnConst))

              // Reset signals in process
              vProcessIf.getStatements.add(new SignalAssignment(stateSig, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
              if (firingGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(firingGapSig, new DecimalLiteral(0)))
              if (iterationGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(iterationGapSig, new DecimalLiteral(0)))
              if (startupGap > 1)
                vProcessIf.getStatements.add(new SignalAssignment(startupGapSig, new DecimalLiteral(0)))
              vProcessIf.getStatements.add(new SignalAssignment(icSig, new DecimalLiteral(0)))
              if (iterationGap > 0 && firingGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(jcSig, new DecimalLiteral(0)))
              vProcessIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral
              (fsmZeros)))

              // State
              val FSM = new CaseStatement(stateSig)
              val fsms = for (i <- 0 until fsmLength)
                yield FSM.createAlternative(new BinaryLiteral(i.toBinaryString.reverse.padTo(fsmLog2, "0").reverse
                  .mkString))

              val stateX = FSM.createAlternative(new BasedLiteral("others"))
              stateX.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(0)))
              stateX.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral(fsmZeros)))

              // Defining States
              var firingGapExists = false
              var iterationGapExists = false
              var startupGapExists = false
              var loopStateCreated = false
              var stateCount = 0

              for (state <- fsms) {
                if (stateCount == 0) { // First State

                  state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral(fsmZeros)))
                  // add this IfStatement to the state 0
                  val vState0If: IfStatement = new IfStatement(new Equals(e.source.wire("vld", ports.get(e.source)
                    .get, null), StdLogic1164.STD_LOGIC_1))
                  if (startupGap > 1) {
                    vState0If.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                    //vState0If.getStatements.add(new SignalAssignment(startupGapSig, new Add(startupGapSig, new
                    // DecimalLiteral(buffer.snkAp.indexOf(1) + 1))))
                  } else {
                    vState0If.getStatements.add(new SignalAssignment(e.wire("re"), readEnConst.getArrayElement(new
                        DecimalLiteral(buffer.snkAp.indexOf(1)))))
                    if (e.target.execTime > 1) vState0If.getStatements.add(new SignalAssignment(icSig, new Add(icSig,
                      new DecimalLiteral(buffer.snkAp.indexOf(1) + 1))))
                    if (iterationGap > 0 && firingGap > 0) vState0If.getStatements.add(new SignalAssignment(jcSig,
                      new Add(jcSig, new DecimalLiteral(buffer.snkAp.indexOf(1) + 1))))
                  }
                  vState0If.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vState0If);

                } else if (startupGap > 1 && startupGapExists == false) {

                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((1).toBinaryString.reverse
                    .padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(startupGapSig, new Add(startupGapSig, new
                      DecimalLiteral(1))))
                  state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))

                  val vState1If = new IfStatement(new Equals(startupGapSig, new DecimalLiteral(startupGap - 1)))
                  vState1If.getStatements.add(new SignalAssignment(startupGapSig, new DecimalLiteral(0)))
                  vState1If.getStatements.add(new SignalAssignment(e.wire("re"), readEnConst.getArrayElement(icSig)))
                  if (e.target.execTime > 1) vState1If.getStatements.add(new SignalAssignment(icSig, new Add(icSig,
                    new DecimalLiteral(1))))
                  if (iterationGap > 0 && firingGap > 0) vState1If.getStatements.add(new SignalAssignment(jcSig, new
                      Add(jcSig, new DecimalLiteral(1))))
                  vState1If.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vState1If);
                  startupGapExists = true

                } else if (loopStateCreated == false) { // The periodic execution state


                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(icSig, new Add(icSig, new DecimalLiteral(1))))
                  state.getStatements.add(new SignalAssignment(e.wire("re"), readEnConst.getArrayElement(icSig)))
                  val vStateNIf = new IfStatement(new Equals(icSig, new DecimalLiteral(e.target.execTime - 1)))

                  if (firingGap > 0) {
                    vStateNIf.getStatements.add(new SignalAssignment(icSig, new DecimalLiteral(0)))
                    vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                    firingGapExists = true
                  } else {
                    vStateNIf.getStatements.add(new SignalAssignment(icSig, new DecimalLiteral(0)))
                    if (iterationGap > 0) {
                      vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                        .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                      iterationGapExists = true
                    }
                    else
                      vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount)
                        .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  }

                  state.getStatements.add(vStateNIf);

                  if (iterationGap > 0 && firingGap > 0) {
                    state.getStatements.add(new SignalAssignment(jcSig, new Add(jcSig, new DecimalLiteral(1))))
                    val vStateIGIf = new IfStatement(new Equals(jcSig, new DecimalLiteral((e.target.execTime * RV.get
                    (e.target).get) - 1)))
                    vStateIGIf.getStatements.add(new SignalAssignment(jcSig, new DecimalLiteral(0)))
                    vStateIGIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 2)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                    state.getStatements.add(vStateIGIf);
                    iterationGapExists = true

                  }

                  loopStateCreated = true

                } else if ((stateCount == fsmLength - 1 - (if (iterationGap > 0) 1 else 0)) && firingGapExists ==
                  true) {

                  state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(firingGapSig, new Add(firingGapSig, new DecimalLiteral
                  (1))))
                  val vStateNIf = new IfStatement(new Equals(firingGapSig, new DecimalLiteral(firingGap - 1)))
                  vStateNIf.getStatements.add(new SignalAssignment(firingGapSig, new DecimalLiteral(0)))
                  vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((loopStartIndex)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vStateNIf);

                } else if (iterationGapExists == true) {

                  state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(iterationGapSig, new Add(iterationGapSig, new
                      DecimalLiteral(1))))
                  val vStateNIf = new IfStatement(new Equals(iterationGapSig, new DecimalLiteral(iterationGap - 1)))
                  vStateNIf.getStatements.add(new SignalAssignment(iterationGapSig, new DecimalLiteral(0)))
                  vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((loopStartIndex)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vStateNIf);

                }

                stateCount = stateCount + 1
              }

              vProcessIf.getElsifParts.get(0).getStatements().add(FSM);

            }

            case 3 => {

              val iterationGap = buffer.ii - ((buffer.snkSchedulingPeriod * (RV.get(e.target).get - 1)) + e.target
                .execTime) // TODO: Update in algorithm
              val firingGap = buffer.snkSchedulingPeriod - e.target.execTime
              val startupGap = buffer.snkStartSchedule - buffer.srcAp.indexOf(1) - 1
              val loopStartIndex = if (startupGap > 1) 2 else 1


              fsmLength = e.target.execTime + loopStartIndex + (if (firingGap > 0) 1 else 0) + (if (iterationGap > 0)
                1 else 0) // corresponds to read signal in state

              val fsmLog2 = Math.ceil(Math.log10(fsmLength) / Math.log10(2)).toInt
              val fsmZeros = Array.fill(fsmLog2)(0).mkString

              // Signal Declaration

              val dinSig = e.target.wire("din", ports.get(e.target).get, e.target.inst + "_" + getChannelPortMap(e
                .portMap)._2 + "_i")
              val dinTempSig = new Signal(e.target.wire("din", ports.get(e.target).get, e.target.inst + "_" +
                getChannelPortMap(e.portMap)._2 + "_i").getIdentifier + "_r", StdLogic1164.STD_LOGIC_VECTOR
              (fifoWidth), Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0))
              if (buffer.size == 1) architecture.getDeclarations.add(new SignalDeclaration(dinTempSig))


              val firingGapSig = new Signal(e.id + "_fgc", Standard.INTEGER, new DecimalLiteral(0))
              val icSig = new Signal(e.id + "_ic", Standard.INTEGER, new DecimalLiteral(0))
              if (firingGap > 0)
                architecture.getDeclarations.add(new SignalDeclaration(icSig))
              if (firingGap > 0)
                architecture.getDeclarations.add(new SignalDeclaration(firingGapSig))
              val iterationGapSig = new Signal(e.id + "_igc", Standard.INTEGER, new DecimalLiteral(0))
              val jcSig = new Signal(e.id + "_jc", Standard.INTEGER, new DecimalLiteral(0))
              if (buffer.size == 1)
                vProcessIf.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                (StdLogic1164.STD_LOGIC_0)))
              if (iterationGap > 0)
                architecture.getDeclarations.add(new SignalDeclaration(iterationGapSig))
              if (iterationGap > 0)
                architecture.getDeclarations.add(new SignalDeclaration(jcSig))
              val startupGapSig = new Signal(e.id + "_sgc", Standard.INTEGER, new DecimalLiteral(0))
              if (startupGap > 1)
                architecture.getDeclarations.add(new SignalDeclaration(startupGapSig))
              val stateSig = new Signal(e.id + "_state", StdLogic1164.STD_LOGIC_VECTOR(fsmLog2), Aggregate.OTHERS
              (StdLogic1164.STD_LOGIC_0))
              architecture.getDeclarations.add(new SignalDeclaration(stateSig))

              // Reset signals in process
              vProcessIf.getStatements.add(new SignalAssignment(stateSig, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
              if (firingGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(firingGapSig, new DecimalLiteral(0)))
              if (firingGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(icSig, new DecimalLiteral(0)))
              if (iterationGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(iterationGapSig, new DecimalLiteral(0)))
              if (iterationGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(jcSig, new DecimalLiteral(0)))
              if (startupGap > 1)
                vProcessIf.getStatements.add(new SignalAssignment(startupGapSig, new DecimalLiteral(0)))
              vProcessIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral
              (fsmZeros)))
              if (buffer.size == 1 && e.srcPort.rate != e.snkPort.rate)
                vProcessIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(dinSig, dinTempSig))
              else if (buffer.size == 1 && e.srcPort.rate == e.snkPort.rate)
                architecture.getStatements.add(new ConditionalSignalAssignment(dinSig, dinTempSig))

              // State
              val FSM = new CaseStatement(stateSig)
              val fsms = for (i <- 0 until fsmLength)
                yield FSM.createAlternative(new BinaryLiteral(i.toBinaryString.reverse.padTo(fsmLog2, "0").reverse
                  .mkString))

              val stateX = FSM.createAlternative(new BasedLiteral("others"))

              if (buffer.size > 1)
                stateX.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
              else {
                if (CI.get(e).get == 0)
                  stateX.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null),
                    StdLogic1164.STD_LOGIC_0))
                stateX.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
              }
              stateX.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral(fsmZeros)))

              // Defining States
              var firingGapExists = false
              var iterationGapExists = false
              var startupGapExists = false
              var stateCount = 0
              val apCountStart = if (startupGap > 1) 0 else buffer.snkAp.indexOf(1)
              var apCount = apCountStart

              for (state <- fsms) {
                if (stateCount == 0) { // First State

                  if (buffer.size > 1)
                    state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  else {
                    if (CI.get(e).get == 0)
                      state.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null)
                        , StdLogic1164.STD_LOGIC_0))
                    state.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                    (StdLogic1164.STD_LOGIC_0)))
                  }
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral(fsmZeros)))
                  // add this IfStatement to the state 0

                  val vState0If: IfStatement = new IfStatement(new Equals(e.source.wire("vld", ports.get(e.source)
                    .get, null), StdLogic1164.STD_LOGIC_1))
                  if (startupGap > 1) {
                    if (buffer.size > 1) vState0If.getStatements.add(new SignalAssignment(e.wire("re"),
                      StdLogic1164.STD_LOGIC_0))
                    else {
                      if (CI.get(e).get == 0)
                        vState0If.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get,
                          null), StdLogic1164.STD_LOGIC_0))
                      vState0If.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                      (StdLogic1164.STD_LOGIC_0)))
                    }
                    if (startupGap > 1)
                      vState0If.getStatements.add(new SignalAssignment(startupGapSig, new Add(startupGapSig, new
                          DecimalLiteral(1))))
                    vState0If.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  } else if (e.target.execTime == 1) {
                    if (buffer.size > 1) vState0If.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(buffer
                      .snkAp(apCount))))
                    else {
                      if (CI.get(e).get == 0)
                        vState0If.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get,
                          null), bitVal(buffer.snkAp(apCount))))
                      vState0If.getStatements.add(new SignalAssignment(dinTempSig, e.source.wire("dout", ports.get(e
                        .source).get, e.source.inst + "_" + getChannelPortMap(e.portMap)._1 + "_o")))
                    }
                    vState0If.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  } else {
                    if (buffer.size > 1) vState0If.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(buffer
                      .snkAp(apCount))))
                    else {
                      if (CI.get(e).get == 0)
                        vState0If.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get,
                          null), bitVal(buffer.snkAp(apCount))))
                      vState0If.getStatements.add(new SignalAssignment(dinTempSig, e.source.wire("dout", ports.get(e
                        .source).get, e.source.inst + "_" + getChannelPortMap(e.portMap)._1 + "_o")))
                    }
                    vState0If.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((loopStartIndex +
                      apCountStart + 1).toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  }

                  state.getStatements.add(vState0If);

                } else if (startupGap > 1 && startupGapExists == false) {

                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((1).toBinaryString.reverse
                    .padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(startupGapSig, new Add(startupGapSig, new
                      DecimalLiteral(1))))
                  if (buffer.size > 1)
                    state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  else {
                    if (CI.get(e).get == 0)
                      state.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null)
                        , StdLogic1164.STD_LOGIC_0))
                    state.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                    (StdLogic1164.STD_LOGIC_0)))
                  }
                  val vState0If = new IfStatement(new Equals(startupGapSig, new DecimalLiteral(startupGap - 1)))
                  vState0If.getStatements.add(new SignalAssignment(startupGapSig, new DecimalLiteral(0)))
                  vState0If.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vState0If)
                  startupGapExists = true

                } /*else if(startupGap > 1 && stateCount == 1 && startupGapExists == false){

                   state.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(buffer.snkReadEnCtrl
                   (startupOffset + stateCount))))
                   state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount+1)
                   .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString))) // if(buffer.snkStartSchedule ==
                   0) 0 else

                 }*/ else if (apCount == (e.target.execTime + apCountStart) - 1) { // The Last State Before Any Delays

                  if (buffer.size > 1) state.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(buffer.snkAp
                  (apCount - apCountStart))))
                  else {
                    if (CI.get(e).get == 0)
                      state.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null)
                        , bitVal(buffer.snkAp(apCount - apCountStart))))
                    state.getStatements.add(new SignalAssignment(dinTempSig, e.source.wire("dout", ports.get(e
                      .source).get, e.source.inst + "_" + getChannelPortMap(e.portMap)._1 + "_o")))
                  }

                  if (firingGap > 0 && iterationGap > 0) {

                    state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))

                    state.getStatements.add(new SignalAssignment(jcSig, new Add(jcSig, new DecimalLiteral(1))))
                    val vStateNIf1 = new IfStatement(new Equals(jcSig, new DecimalLiteral(RV.get(e.target).get - 1)))
                    vStateNIf1.getStatements.add(new SignalAssignment(jcSig, new DecimalLiteral(0)))
                    vStateNIf1.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 2)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                    state.getStatements.add(vStateNIf1);

                    firingGapExists = true
                    iterationGapExists = true

                  } else if (firingGap > 0) {

                    state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                    firingGapExists = true

                  } else if (iterationGap > 0) {

                    state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((loopStartIndex)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                    state.getStatements.add(new SignalAssignment(jcSig, new Add(jcSig, new DecimalLiteral(1))))
                    val vStateNIf = new IfStatement(new Equals(jcSig, new DecimalLiteral(RV.get(e.target).get - 1)))
                    vStateNIf.getStatements.add(new SignalAssignment(jcSig, new DecimalLiteral(0)))
                    vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                    state.getStatements.add(vStateNIf)
                    iterationGapExists = true

                  } else {

                    state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))

                  }

                  apCount = apCount + 1
                } else if ((stateCount == fsmLength - 1 - (if (iterationGap > 0) 1 else 0)) && firingGapExists ==
                  true) {

                  if (buffer.size > 1) state.getStatements.add(new SignalAssignment(e.wire("re"),
                    StdLogic1164.STD_LOGIC_0))
                  else {
                    if (CI.get(e).get == 0)
                      state.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null)
                        , StdLogic1164.STD_LOGIC_0))
                    state.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                    (StdLogic1164.STD_LOGIC_0)))
                  }
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(firingGapSig, new Add(firingGapSig, new DecimalLiteral
                  (1))))
                  val vStateNIf = new IfStatement(new Equals(firingGapSig, new DecimalLiteral(firingGap - 1)))
                  vStateNIf.getStatements.add(new SignalAssignment(firingGapSig, new DecimalLiteral(0)))
                  vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((loopStartIndex)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vStateNIf);

                } else if (iterationGapExists == true) {

                  if (buffer.size > 1)
                    state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  else {
                    if (CI.get(e).get == 0)
                      state.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null)
                        , StdLogic1164.STD_LOGIC_0))
                    state.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                    (StdLogic1164.STD_LOGIC_0)))
                  }
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(iterationGapSig, new Add(iterationGapSig, new
                      DecimalLiteral(1))))
                  val vStateNIf = new IfStatement(new Equals(iterationGapSig, new DecimalLiteral(iterationGap - 1)))
                  vStateNIf.getStatements.add(new SignalAssignment(iterationGapSig, new DecimalLiteral(0)))
                  vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((loopStartIndex)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vStateNIf);

                } else if (apCount < (e.target.execTime + apCountStart)) { // Intermediate States

                  if (buffer.size > 1)
                    state.getStatements.add(new SignalAssignment(e.wire("re"), bitVal(buffer.snkAp(apCount -
                      apCountStart))))
                  else {
                    if (CI.get(e).get == 0)
                      state.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null)
                        , bitVal(buffer.snkAp(apCount - apCountStart))))
                    state.getStatements.add(new SignalAssignment(dinTempSig, e.source.wire("dout", ports.get(e
                      .source).get, e.source.inst + "_" + getChannelPortMap(e.portMap)._1 + "_o")))
                  }
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))

                  apCount = apCount + 1

                } else { // last state before condition = if(apCount < e.target.execTime)

                  if (buffer.size > 1)
                    state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  else {
                    if (CI.get(e).get == 0)
                      state.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null)
                        , StdLogic1164.STD_LOGIC_0))
                    state.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                    (StdLogic1164.STD_LOGIC_0)))
                  }
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString))) // if(buffer.snkStartSchedule ==
                  // 0) 0 else

                }
                stateCount = stateCount + 1
              }

              vProcessIf.getElsifParts.get(0).getStatements().add(FSM);

            }

            case 4 => {

              //val snkLastStepSize = buffer.ii - (buffer.snkSchedulingPeriod * RV.get(e.target).get) + buffer
              // .snkSchedulingPeriod
              val iterationGap = buffer.ii - ((buffer.snkSchedulingPeriod * (RV.get(e.target).get - 1)) + e.target
                .execTime) // TODO: Update in algorithm
              val firingGap = buffer.snkSchedulingPeriod - e.target.execTime
              val startupGap = buffer.snkStartSchedule - buffer.srcAp.indexOf(1) - 1 // minus 1 for ReadEn
              val loopStartIndex = if (startupGap > 1) 2 else 1

              fsmLength = (if (startupGap > 1) 1 else 0) + (if (firingGap > 0) 1 else 0) + (if (iterationGap > 0) 1
              else 0) + 2 // corresponds to read signal in state

              val fsmLog2 = Math.ceil(Math.log10(fsmLength) / Math.log10(2)).toInt
              val fsmZeros = Array.fill(fsmLog2)(0).mkString

              // Signal Declaration

              val dinSig = e.target.wire("din", ports.get(e.target).get, e.target.inst + "_" + getChannelPortMap(e
                .portMap)._2 + "_i")
              val dinTempSig = new Signal(e.target.wire("din", ports.get(e.target).get, e.target.inst + "_" +
                getChannelPortMap(e.portMap)._2 + "_i").getIdentifier + "_r", StdLogic1164.STD_LOGIC_VECTOR
              (fifoWidth), Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0))
              if (buffer.size == 1)
                architecture.getDeclarations.add(new SignalDeclaration(dinTempSig))

              val firingGapSig = new Signal(e.id + "_fgc", Standard.INTEGER, new DecimalLiteral(0))
              if (firingGap > 0)
                architecture.getDeclarations.add(new SignalDeclaration(firingGapSig))
              val icSig = new Signal(e.id + "_ic", Standard.INTEGER, new DecimalLiteral(0))
              architecture.getDeclarations.add(new SignalDeclaration(icSig))
              val iterationGapSig = new Signal(e.id + "_igc", Standard.INTEGER, new DecimalLiteral(0))
              val jcSig = new Signal(e.id + "_jc", Standard.INTEGER, new DecimalLiteral(0))
              if (iterationGap > 0)
                architecture.getDeclarations.add(new SignalDeclaration(iterationGapSig))
              if (iterationGap > 0 && firingGap > 0)
                architecture.getDeclarations.add(new SignalDeclaration(jcSig))
              val startupGapSig = new Signal(e.id + "_sgc", Standard.INTEGER, new DecimalLiteral(0))
              if (startupGap > 1)
                architecture.getDeclarations.add(new SignalDeclaration(startupGapSig))
              val stateSig = new Signal(e.id + "_state", StdLogic1164.STD_LOGIC_VECTOR(fsmLog2), Aggregate.OTHERS
              (StdLogic1164.STD_LOGIC_0))
              architecture.getDeclarations.add(new SignalDeclaration(stateSig))

              val readEnVec = buffer.snkAp.mkString //buffer.snkReadEnCtrl.slice(buffer.snkStartSchedule - 1, buffer
              // .snkStartSchedule + e.target.execTime).mkString
              val readEnConst = new Constant(e.id + "_cp", StdLogic1164.STD_LOGIC_VECTOR(readEnVec.length), new
                  BinaryLiteral(readEnVec.reverse))
              architecture.getDeclarations.add(new ConstantDeclaration(readEnConst))

              // sequential

              // Reset signals in process
              vProcessIf.getStatements.add(new SignalAssignment(stateSig, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
              if (firingGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(firingGapSig, new DecimalLiteral(0)))
              if (iterationGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(iterationGapSig, new DecimalLiteral(0)))
              if (startupGap > 1)
                vProcessIf.getStatements.add(new SignalAssignment(startupGapSig, new DecimalLiteral(0)))
              vProcessIf.getStatements.add(new SignalAssignment(icSig, new DecimalLiteral(0)))
              if (buffer.size == 1)
                vProcessIf.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                (StdLogic1164.STD_LOGIC_0)))
              if (iterationGap > 0 && firingGap > 0)
                vProcessIf.getStatements.add(new SignalAssignment(jcSig, new DecimalLiteral(0)))
              vProcessIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral
              (fsmZeros)))
              if (buffer.size == 1 && e.srcPort.rate != e.snkPort.rate)
                vProcessIf.getElsifParts.get(0).getStatements.add(new SignalAssignment(dinSig, dinTempSig))
              else if (buffer.size == 1 && e.srcPort.rate == e.snkPort.rate)
                architecture.getStatements.add(new ConditionalSignalAssignment(dinSig, dinTempSig))

              // State
              val FSM = new CaseStatement(stateSig)
              val fsms = for (i <- 0 until fsmLength)
                yield FSM.createAlternative(new BinaryLiteral(i.toBinaryString.reverse.padTo(fsmLog2, "0").reverse
                  .mkString))

              val stateX = FSM.createAlternative(new BasedLiteral("others"))
              if (buffer.size > 1)
                stateX.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
              else {
                if (CI.get(e).get == 0)
                  stateX.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null),
                    StdLogic1164.STD_LOGIC_0))
                stateX.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0)))
              }
              stateX.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral(fsmZeros)))

              // Defining States
              var firingGapExists = false
              var iterationGapExists = false
              var startupGapExists = false
              var loopStateCreated = false
              var stateCount = 0

              for (state <- fsms) {
                if (stateCount == 0) { // First State

                  if (buffer.size > 1)
                    state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  else {
                    if (CI.get(e).get == 0)
                      state.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null)
                        , StdLogic1164.STD_LOGIC_0))
                    state.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                    (StdLogic1164.STD_LOGIC_0)))
                  }
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral(fsmZeros)))
                  // add this IfStatement to the state 0
                  val vState0If: IfStatement = new IfStatement(new Equals(e.source.wire("vld", ports.get(e.source)
                    .get, null), StdLogic1164.STD_LOGIC_1))
                  if (startupGap > 1) {
                    // vState0If.getStatements.add(new SignalAssignment(startupGapSig, new Add(startupGapSig, new
                    // DecimalLiteral(buffer.snkAp.indexOf(1) + 1))))
                    if (buffer.size > 1) vState0If.getStatements.add(new SignalAssignment(e.wire("re"),
                      StdLogic1164.STD_LOGIC_0))
                    else {
                      if (CI.get(e).get == 0)
                        vState0If.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get,
                          null), StdLogic1164.STD_LOGIC_0))
                      vState0If.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                      (StdLogic1164.STD_LOGIC_0)))
                    }
                  } else {
                    if (buffer.size > 1)
                      vState0If.getStatements.add(new SignalAssignment(e.wire("re"), readEnConst.getArrayElement
                      (icSig)))
                    else {
                      if (CI.get(e).get == 0)
                        vState0If.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get,
                          null), readEnConst.getArrayElement(icSig)))
                      // if(e.target.execTime > 1) vState0If.getStatements.add(new SignalAssignment(icSig, new Add
                      // (icSig, new DecimalLiteral(buffer.snkAp.indexOf(1) + 1))))
                      // if(iterationGap > 0 && firingGap > 0) vState0If.getStatements.add(new SignalAssignment
                      // (jcSig, new Add(jcSig, new DecimalLiteral(buffer.snkAp.indexOf(1) + 1))))
                      vState0If.getStatements.add(new SignalAssignment(dinTempSig, e.source.wire("dout", ports.get(e
                        .source).get, e.source.inst + "_" + getChannelPortMap(e.portMap)._1 + "_o")))
                    }

                    if (e.target.execTime > 1)
                      vState0If.getStatements.add(new SignalAssignment(icSig, new Add(icSig, new DecimalLiteral(1))))
                    if (iterationGap > 0 && firingGap > 0)
                      vState0If.getStatements.add(new SignalAssignment(jcSig, new Add(jcSig, new DecimalLiteral(1))))
                  }
                  vState0If.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vState0If);

                } else if (startupGap > 1 && startupGapExists == false) {

                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((1).toBinaryString.reverse
                    .padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(startupGapSig, new Add(startupGapSig, new
                      DecimalLiteral(1))))
                  if (buffer.size > 1)
                    state.getStatements.add(new SignalAssignment(e.wire("re"), StdLogic1164.STD_LOGIC_0))
                  else {
                    if (CI.get(e).get == 0)
                      state.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null)
                        , StdLogic1164.STD_LOGIC_0))
                    state.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                    (StdLogic1164.STD_LOGIC_0)))
                  }

                  val vState1If = new IfStatement(new Equals(startupGapSig, new DecimalLiteral(startupGap - 1)))
                  vState1If.getStatements.add(new SignalAssignment(startupGapSig, new DecimalLiteral(0)))
                  if (buffer.size > 1)
                    vState1If.getStatements.add(new SignalAssignment(e.wire("re"), readEnConst.getArrayElement(icSig)))
                  else {
                    if (CI.get(e).get == 0)
                      vState1If.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get,
                        null), readEnConst.getArrayElement(icSig)))
                    vState1If.getStatements.add(new SignalAssignment(dinTempSig, e.source.wire("dout", ports.get(e
                      .source).get, e.source.inst + "_" + getChannelPortMap(e.portMap)._1 + "_o")))

                  } //

                  if (e.target.execTime > 1)
                    vState1If.getStatements.add(new SignalAssignment(icSig, new Add(icSig, new DecimalLiteral(1))))
                  if (iterationGap > 0 && firingGap > 0)
                    vState1If.getStatements.add(new SignalAssignment(jcSig, new Add(jcSig, new DecimalLiteral(1))))
                  vState1If.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vState1If);
                  startupGapExists = true

                } else if (loopStateCreated == false) { // The periodic execution state


                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(icSig, new Add(icSig, new DecimalLiteral(1))))
                  if (buffer.size > 1)
                    state.getStatements.add(new SignalAssignment(e.wire("re"), readEnConst.getArrayElement(icSig)))
                  else {
                    if (CI.get(e).get == 0)
                      state.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null)
                        , readEnConst.getArrayElement(icSig)))
                    state.getStatements.add(new SignalAssignment(dinTempSig, e.source.wire("dout", ports.get(e
                      .source).get, e.source.inst + "_" + getChannelPortMap(e.portMap)._1 + "_o")))
                    // val vStateN0f = new IfStatement(new Equals(readEnConst.getArrayElement(icSig),
                    // StdLogic1164.STD_LOGIC_0))
                    // vStateN0f.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                    // (StdLogic1164.STD_LOGIC_0)))
                    // state.getStatements.add(vStateN0f);
                  }
                  val vStateNIf = new IfStatement(new Equals(icSig, new DecimalLiteral(e.target.execTime - 1)))

                  if (firingGap > 0) {
                    vStateNIf.getStatements.add(new SignalAssignment(icSig, new DecimalLiteral(0)))
                    vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                    firingGapExists = true
                  } else {
                    vStateNIf.getStatements.add(new SignalAssignment(icSig, new DecimalLiteral(0)))
                    if (iterationGap > 0) {
                      vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 1)
                        .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                      iterationGapExists = true
                    }
                    else
                      vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount)
                        .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  }

                  state.getStatements.add(vStateNIf);

                  if (iterationGap > 0 && firingGap > 0) {
                    state.getStatements.add(new SignalAssignment(jcSig, new Add(jcSig, new DecimalLiteral(1))))
                    val vStateIGIf = new IfStatement(new Equals(jcSig, new DecimalLiteral((e.target.execTime * RV.get
                    (e.target).get) - 1)))
                    vStateIGIf.getStatements.add(new SignalAssignment(jcSig, new DecimalLiteral(0)))
                    vStateIGIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount + 2)
                      .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                    state.getStatements.add(vStateIGIf);
                    iterationGapExists = true

                  }

                  loopStateCreated = true

                } else if ((stateCount == fsmLength - 1 - (if (iterationGap > 0) 1 else 0)) && firingGapExists ==
                  true) {

                  if (buffer.size > 1) state.getStatements.add(new SignalAssignment(e.wire("re"),
                    StdLogic1164.STD_LOGIC_0))
                  else {
                    if (CI.get(e).get == 0)
                      state.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null)
                        , StdLogic1164.STD_LOGIC_0))
                    state.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                    (StdLogic1164.STD_LOGIC_0)))
                  }

                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(firingGapSig, new Add(firingGapSig, new DecimalLiteral
                  (1))))
                  val vStateNIf = new IfStatement(new Equals(firingGapSig, new DecimalLiteral(firingGap - 1)))
                  vStateNIf.getStatements.add(new SignalAssignment(firingGapSig, new DecimalLiteral(0)))
                  vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((loopStartIndex)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vStateNIf);

                } else if (iterationGapExists == true) {

                  if (buffer.size > 1) state.getStatements.add(new SignalAssignment(e.wire("re"),
                    StdLogic1164.STD_LOGIC_0))
                  else {
                    if (CI.get(e).get == 0)
                      state.getStatements.add(new SignalAssignment(e.target.wire("en", ports.get(e.target).get, null)
                        , StdLogic1164.STD_LOGIC_0))
                    state.getStatements.add(new SignalAssignment(dinTempSig, Aggregate.OTHERS
                    (StdLogic1164.STD_LOGIC_0)))
                  }
                  state.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((stateCount)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(new SignalAssignment(iterationGapSig, new Add(iterationGapSig, new
                      DecimalLiteral(1))))
                  val vStateNIf = new IfStatement(new Equals(iterationGapSig, new DecimalLiteral(iterationGap - 1)))
                  vStateNIf.getStatements.add(new SignalAssignment(iterationGapSig, new DecimalLiteral(0)))
                  vStateNIf.getStatements.add(new SignalAssignment(stateSig, new BinaryLiteral((loopStartIndex)
                    .toBinaryString.reverse.padTo(fsmLog2, "0").reverse.mkString)))
                  state.getStatements.add(vStateNIf);

                }

                stateCount = stateCount + 1
              }

              vProcessIf.getElsifParts.get(0).getStatements().add(FSM);

            }


            case _ => null

          }


          process.getStatements.add(vProcessIf)
          // modify the sensitivity list
          process.getSensitivityList.add(rst)
          process.getSensitivityList.add(clk)
          architecture.getStatements.add(process)

          if (endActors.contains(n)) lastChannelPortMap = e.portMap

          totalFsms = totalFsms + fsmLength

        }

        /** root actor operations */
        if (endActors.contains(n)) {
          n.component.getPort.flatMap(e => e.getVhdlObjects).filter(_.getMode == Mode.OUT)
            .foreach { s =>

              /** extract input ports from the root actor and add them to toplevel entity */
              entity.getPort.add(new Signal(s.getIdentifier + topOutputSignalIdentifier_i, s.getMode,
                getEntitySignalType(n, s)))

              /** register assignment ralating to root actor */
              architecture.getStatements.add(new ConditionalSignalAssignment(new Signal(s.getIdentifier +
                topOutputSignalIdentifier_i, Standard.INTEGER, new DecimalLiteral(0)), new Signal(n.inst + "_" + s
                .getIdentifier + getSignalMode(s), StdLogic1164.STD_LOGIC_VECTOR(0))))
            }
          topOutputSignalIdentifier_i = topOutputSignalIdentifier_i + 1
        }

      }
      // println("Total FSMs = " + totalFsms)

      architecture.getDeclarations.distinct
      architecture
    }

    (createFile, totalFsms)
  }

  def dse(prjDir: String, topFile: String, len: Int, libClauses: List[String], useClauses: List[String]): Unit = {
    val min = 0.0
    val max = graph.maxThr
    val length = len
    val thrs = (min to max by (max - min) / (length).toDouble).toList
    println(thrs)
    val pw = new PrintWriter(new File(prjDir + topFile + ".csv"))
    pw.write("Throughput,Optimization,Number of Slice Registers,% Number of Slice Registers,Number of Slice LUTs,% " +
      "Number of Slice LUTs,Number of occupied Slices,% Number of occupied Slices,Number of RAMB16BWERs,% Number of " +
      "RAMB16BWERs,Number of RAMB8BWERs,% Number of RAMB8BWERs,Number of BUFG/BUFGMUXs,% Number of BUFG/BUFGMUXs," +
      "Number of DSP48A1s,% Number of DSP48A1s,Total Area,Total Buffer Size,Frequency,Power,#Actors,#Channels," +
      "#Channels [buffer size = 1],Latency,Iteration Period,Buffer Allocation Time (ms),HDL Generation Elapsed Time " +
      "(ms),Synthesis Elapsed Time (ms),Total Elapsed Time (ms),Total FSMs,Total Code Lines\n")

    var count = 0
    for (thr <- thrs) {
      if (thr > 0.0) {

        val bufferAllocT0 = System.currentTimeMillis
        val slots = graph.bufferSlots(thr)
        val bufferAllocT1 = System.currentTimeMillis
        var bufferAllocTimeMS = bufferAllocT1 - bufferAllocT0

        for (opt <- 1 to 4) {
          val file = new File(prjDir + topFile + ".vhd")
          val bw = new BufferedWriter(new FileWriter(file))

          val hdlGenT0 = System.currentTimeMillis
          val vhdlFile = toplevel(slots, topFile, libClauses, useClauses, opt)
          VhdlOutput.toWriter(vhdlFile._1, bw)
          bw.close
          val hdlGenT1 = System.currentTimeMillis

          val hdlGenElapsedTimeMS = hdlGenT1 - hdlGenT0

          val synthT0 = System.currentTimeMillis

          val synthLines = Process("/usr/local/bin/XilinxISE/14.7/ISE_DS/ISE/bin/lin/xtclsh " + prjDir + topFile + "" +
            ".tcl rebuild_project").lineStream
          synthLines.foreach(l => println("Benckmark: " + count + ": " + l))

          val synthT1 = System.currentTimeMillis
          val synthElapsedTimeMS = synthT1 - synthT0

          val areaKeys = List("Number of Slice Registers:",
            "Number of Slice LUTs:",
            "Number of occupied Slices:",
            "Number of RAMB16BWERs:",
            "Number of RAMB8BWERs:",
            "Number of BUFG/BUFGMUXs:",
            "Number of DSP48A1s:")
          val freqKeys = List("Maximum Frequency:")
          val pwrKeys = List("| Total                 |")

          val areaMap = synthLines.filter(l => areaKeys.exists(l.contains)).distinct.map { f =>
            (f.substring(f.indexOf("Number of"), f.indexOf(":")).trim(),
              ("""( \d+(?:\,\d+ )?)""".r findAllIn f).toList)
          }.toMap

          val freqList = synthLines.filter(l => freqKeys.exists(l.contains)).distinct.map { f =>
            (f.substring(f.indexOf("Frequency:") + 10, f.indexOf("MHz")).trim()).trim().replaceAll(",", "").toDouble
          }.toList

          val pwrList = synthLines.filter(l => pwrKeys.exists(l.contains)).distinct.map { f =>
            ("""(\d+(?:\.\d+)?)""".r findAllIn f).toList
          }

          if (!areaMap.isEmpty && !freqList.isEmpty && !pwrList.isEmpty) {
            pw.write(thr + ",") // Throughput
            pw.write(opt + ",") // Optimization
            pw.write(areaMap.get("Number of Slice Registers").get(0).trim.replaceAll(",", "").toInt + ",")
            pw.write(areaMap.get("Number of Slice Registers").get(2).trim.toInt + ",")
            pw.write(areaMap.get("Number of Slice LUTs").get(0).trim.replaceAll(",", "").toInt + ",")
            pw.write(areaMap.get("Number of Slice LUTs").get(2).trim.toInt + ",")
            pw.write(areaMap.get("Number of occupied Slices").get(0).trim.replaceAll(",", "").toInt + ",")
            pw.write(areaMap.get("Number of occupied Slices").get(2).trim.toInt + ",")
            pw.write(areaMap.get("Number of RAMB16BWERs").get(0).trim.replaceAll(",", "").toInt + ",")
            pw.write(areaMap.get("Number of RAMB16BWERs").get(2).trim.toInt + ",")
            pw.write(areaMap.get("Number of RAMB8BWERs").get(0).trim.replaceAll(",", "").toInt + ",")
            pw.write(areaMap.get("Number of RAMB8BWERs").get(2).trim.toInt + ",")
            pw.write(areaMap.get("Number of BUFG/BUFGMUXs").get(0).trim.replaceAll(",", "").toInt + ",")
            pw.write(areaMap.get("Number of BUFG/BUFGMUXs").get(2).trim.toInt + ",")
            pw.write(areaMap.get("Number of DSP48A1s").get(0).trim.replaceAll(",", "").toInt + ",")
            pw.write(areaMap.get("Number of DSP48A1s").get(2).trim.toInt + ",")
            pw.write(areaMap.map(_._2(0).trim.replaceAll(",", "").toInt).toList.reduceLeft(_ + _) + ",") // AreaSum
            pw.write(slots.map(_._2.size).toList.reduceLeft(_ + _) + ",") // BufferSize
            pw.write(freqList.head + ",") // Frequency
            pw.write(pwrList.toList.head.head.toDouble + ",") // Power
            pw.write(graph.order + ",") // #Actors
            pw.write(graph.size + ",") // #Channels
            pw.write(slots.count({ case (k, v) => v.size == 1 }) + ",") // #Channels [buffer size = 1]
            pw.write(graph.latency(slots) + ",") // Latency
            pw.write(graph.period(thr) + ",") // Iteration Period
            pw.write(bufferAllocTimeMS + ",") // Buffer Allocation Time (ms)
            pw.write(hdlGenElapsedTimeMS + ",") // HDL Generation Elapsed Time
            pw.write(synthElapsedTimeMS + ",") // Synthesis Elapsed Time
            pw.write((bufferAllocTimeMS + hdlGenElapsedTimeMS + synthElapsedTimeMS) + ",") // Total Elapsed Time
            pw.write(vhdlFile._2 + ",") // Total FSMs
            pw.write(io.Source.fromFile(prjDir + topFile + ".vhd").getLines.size + "\n") //Total Code Lines
            bufferAllocTimeMS = 0
          }
          count = count + 1
        }
      }
    }
    pw.close
  }

  def dse_v2(prjDir: String, topFile: String, len: Int, libClauses: List[String], useClauses: List[String]): Unit = {
    val min = 0.0
    val max = graph.maxThr
    val length = len
    val thrs = (min to max by (max - min) / (length).toDouble).toList
    println(thrs)
    val pw = new PrintWriter(new File(prjDir + topFile + "_fsms_and_lines.csv"))
    pw.write("Throughput,Optimization,Total FSMs,Total Code Lines\n")

    var count = 0
    for (thr <- thrs) {
      if (thr > 0.0) {

        val slots = graph.bufferSlots(thr)

        for (opt <- 0 to 4) {
          val file = new File(prjDir + topFile + ".vhd")
          val bw = new BufferedWriter(new FileWriter(file))

          val vhdlFile = toplevel(slots, topFile, libClauses, useClauses, opt)
          VhdlOutput.toWriter(vhdlFile._1, bw)
          bw.close

          pw.write(thr + ",") // Throughput
          pw.write(opt + ",") // Optimization
          pw.write(vhdlFile._2 + ",") // Total FSMs
          pw.write(io.Source.fromFile(prjDir + topFile + ".vhd").getLines.size + "\n") //Total Code Lines

          count = count + 1
        }
      }
    }
    pw.close
  }
   */
}
