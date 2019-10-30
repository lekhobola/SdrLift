package sdrlift.model

//Graph
import scalax.collection.GraphPredef._
import scalax.collection.GraphEdge._
import scalax.collection.Graph
import scalax.collection.io.dot._
import scalax.collection.io.dot.implicits._

import scala.collection.mutable.ArraySeq
import java.io.PrintWriter

import exp.KernelExp.KernelPort
import sdrlift.model.Channel.FifoProps

import sys.process._
import scala.language.postfixOps
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.annotation.migration

object Sdfap {

  implicit class ExtGraph[N, E[X] <: EdgeLikeIn[X]](g: Graph[Actor, Channel]) {
    /**
      * TODO **
      * Build the topology starting with the firable node... That is, the node with
      * inital tokens in its input edges enough for it to fire.
      *
      */
    def topologyM: Matrix[Int] = {
      var i = 0
      val m = scala.collection.mutable.ArraySeq.fill(g.graphSize)((g nodes) map { n => (n -> 0) } toMap)

      (g.edges) foreach {
        e => {
          m(i) = m(i) + (e.source -> e.srcPort.rate, e.target -> -1 * e.snkPort.rate);
          i += 1
        }
      }
      Matrix.unflatten(g.order, m.flatMap({ case k => k.values toList }))
    }

    private def getRootNodes: List[g.NodeT] = {
      (g nodes).filter(p => p.incoming.isEmpty).toList
    }

    def getRootActors: List[Actor] = {
      getRootNodes.map { n => Actor(n.id, n.inst, n.params, n.execTime) }
    }

    private def getEndNodes: List[g.NodeT] = {
      (g nodes).filter(p => p.outgoing.isEmpty).toList
    }

    def getEndActors: List[Actor] = {
      getEndNodes.map { n => Actor(n.id, n.inst, n.params, n.execTime) }
    }

    private def getPassEndEdge: List[g.EdgeT] = {
      getEndNodes.last.incoming.toList
    }


    private def getOrderedNodes: List[g.NodeT] = {
      val roots = getRootNodes
      var iSeq = IndexedSeq[(Int, g.NodeT)]()
      for (r <- roots) {
        val xs = r.innerNodeTraverser.map(n => n).toSeq
        val seq = (xs.indices zip xs)
        iSeq = iSeq ++ seq
      }

      iSeq.toList.sortBy(_._1).distinct.map(_._2)
    }

    def getOrderedActors: List[Actor] = {
      getOrderedNodes.map { n => Actor(n.id, n.inst, n.params, n.execTime) }
    }

    def incomingChannels(a: Actor): List[Channel[Actor]] = {
      val n = g get a
      n.incoming.map { e =>
        Channel[Actor](e.srcActor, e.snkActor, e.id, e.srcPort, e.snkPort, e.dly, e.portMap)
      }.toList
    }

    def maxThr: Double = {
      val endRate = getEndNodes.head.incoming.head.snkPort.rate
      val actor = getOrderedNodes.maxBy(f => f.execTime) //actor with longest execution time
      val maxExecTime = actor.execTime
      val thr = endRate.toDouble / maxExecTime.toDouble

      thr
    }

    private def firingM: Map[g.NodeT, Int] = {
      // greatest common divisor
      def gcd(a: Int, b: Int): Int = if (b == 0) a.abs else gcd(b, a % b)

      // initialize a map of actors and firing frequency=0
      var m = (g nodes).map { n => (n -> 0) }.toMap
      // calculate number of times each actor fires starting with a root actor
      val k = getOrderedNodes map { n =>
        n.incoming foreach {
          e => {
            // number of current channel tokens
            val edgeTokenCount = e.dly + (e.srcPort.rate * m(e.source))
            // number of source actor firings
            val srcFires = if (edgeTokenCount > 0) (edgeTokenCount / e.srcPort.rate) else e.snkPort.rate
            // number of target actor firings
            val snkFires = if (edgeTokenCount > 0) (edgeTokenCount / e.snkPort.rate) else e.srcPort.rate
            // update the firing map
            m = m + (e.source -> srcFires, e.target -> snkFires)
          }
        }
      }

      // get the list of firings
      val rates = (m.toList map { case (k, v) => v }).toList
      // find the GCD of the list
      val cd = rates.reduceLeft(gcd(_, _))
      // divide firing map by GCD
      for ((k, v) <- m) {
        m = m + (k -> v / cd)
      }
      m
    }

    def repetitionM(rootRep: Int): Map[Actor, Int] = {
      computeRepetitions(rootRep).map {
        case (k, v) => {
          val actor = Actor(k.id, k.inst, k.params,k.execTime)
          (actor, v)
        }
      }
    }

    private def repetitionV(reps: Map[Actor, Int]): Matrix[Int] = {
      // get the list of firings
      val m = (reps.toList map {
        case (k, v) => v
      }).toList
      // return the repetition vector  = [size(m) x 1 matrix]
      Matrix.unflatten(1, ArraySeq(m: _*))
    }

    def isRepetitionValid(fires : Map[Actor, Int] ): Boolean = {
      val root = getEndActors
      //val fires = repetitionM
      val timestampBefore = getOrderedNodes.flatMap {
        n => n.incoming
      }.filter { !_.isEmpty }.map {
        e => e -> e.dly
      }.toMap

      var timestampAfter = timestampBefore

      getOrderedNodes.map {
        n =>
        {
          for (i <- n.incoming.toList) {
            val inputToken = timestampAfter(i) - (fires(n) * i.snkPort.rate)
            timestampAfter = timestampAfter + (i -> inputToken)
          }
          for (o <- n.outgoing.toList) {
            val outputToken = timestampAfter(o) + (fires(n) * o.srcPort.rate)
            timestampAfter = timestampAfter + (o -> outputToken)
          }
        }
      }
      timestampBefore == timestampAfter
    }

    // Channel Buffer
    def channelBuffer(src: Actor,
                        snk: Actor,
                        srcPort: KernelPort,
                        snkPort: KernelPort,
                        srcRepetition : Int,
                        snkRepetition : Int,
                        ii : Int,
                        rootSchedulePeriod: Int,
                        shortedPredecessorSP: Int) : FifoProps = { // largest biggest source scheduling period (SP) before successor actors with consumption and production rates of 1

      val roots = getRootActors
      /** set the source scheduling period
        *  if schduling of a root node is 0 : value = (initiation interval) / (repetition vector of source actor)
        *  else : value = the sink scheduling period of the preceding channel in the schedule
        */
      var srcSchedulePeriod = if(roots.contains(src)) Math.floor(ii.toDouble / srcRepetition.toDouble).toInt else rootSchedulePeriod  // TODO: verify this
      srcSchedulePeriod = if(srcSchedulePeriod < src.execTime) src.execTime else srcSchedulePeriod
      // set the source 0-index schedule
      val srcStartSchedule = 0

      /**
        * create an access patterns as an array of binary values
        */
      val srcAp : List[Int] = (((srcPort.ap flatMap{ e => List.fill(e._1)(e._2) }).toList).map{e => e.toList }.flatten) map(_.asDigit)
      val snkAp : List[Int] = (((snkPort.ap flatMap{ e => List.fill(e._1)(e._2) }).toList).map{e => e.toList }.flatten) map(_.asDigit)

      /**
        *  Temporary
        */

      var srcScheduleTmp = Array.fill(srcSchedulePeriod * srcRepetition)(0)
      for (i <-0 until srcSchedulePeriod * srcRepetition by srcSchedulePeriod){

        for(j <- i  until i + src.execTime)
          if(j < srcSchedulePeriod * srcRepetition) srcScheduleTmp(j) = srcAp((j - i))

      }
      //println("srcScheduleTmp Completed!")
      val srcAccBufferSumTmp = (srcScheduleTmp.scanLeft(0)(_+_)).slice(1, srcSchedulePeriod * srcRepetition + 1)

      var snkStartSchedule  = (srcAccBufferSumTmp.indexOf(snkPort.rate)) - snkPort.rate + 1

      // TODO: Update in the algorithm
      // Ensures that the snkStartSchedule >= 1
      snkStartSchedule = if(snkStartSchedule <= 0)
        snk.execTime-1 /* else if(snkStartSchedule == 0)  1 */
      else if((snkStartSchedule + snkRepetition) < (srcSchedulePeriod*srcRepetition) && snkStartSchedule == 1 && !srcAp.mkString.equals(snkAp.mkString)) // TODO: Latest
        Math.ceil((srcSchedulePeriod*srcRepetition) - (snkPort.rate*snkRepetition)).toInt
      else
        snkStartSchedule

      val snkSecondSchedule =  (srcAccBufferSumTmp.indexOf(2 * snkPort.rate)) - snkPort.rate + 1  //(srcAccBufferSumTmp.indexOf(2 * snkPort.rate)) - snkPort.rate + 1

      val snkSchedulePeriod =if(snk.execTime > 1 && shortedPredecessorSP > 0)
        shortedPredecessorSP
      else if(snkSecondSchedule < snkStartSchedule || (snkSecondSchedule - snkStartSchedule) < snk.execTime)
        Math.floor(ii / snkRepetition).toInt
      else
        snkSecondSchedule - snkStartSchedule

      // TODO: update this in the algorithm
      //val schedulePeriod    = if(snkStartSchedule <= 0) snk.execTime + ii else snkStartSchedule + ii //else snkStartSchedule + ii
      val schedulePeriod    = snkStartSchedule + (snkSchedulePeriod * snkRepetition)                       //   if(snkStartSchedule <= 0) snk.execTime + (snkSchedulePeriod * snkRepetition) else  snkStartSchedule + (snkSchedulePeriod * snkRepetition)


      // Source Schedule
      // TODO: update this in the agorithm
      val srcSchedule = Array.fill(schedulePeriod)(0)
      for (i <- srcStartSchedule until schedulePeriod by srcSchedulePeriod){

        for( j <- i  until i + src.execTime )
          if(j < schedulePeriod) srcSchedule(j) = srcAp(j - i)
      }
      //println("srcSchedule Completed!")
      val srcAccBufferSum =  (srcSchedule.scanLeft(0)(_+_)).toList

      // Sink Schedule
      //val snkApEndLen = schedulePeriod - (snkSchedulePeriod * snkRepetition)
      val snkSchedule = Array.fill(schedulePeriod)(0)
      for (i <- snkStartSchedule until schedulePeriod by snkSchedulePeriod){

        for( j <- i  until i + snk.execTime )
          if(j < schedulePeriod) snkSchedule(j) =  snkAp((j - i))

      }
      //println("snkSchedule Completed!")
      val snkAccBufferSum = (snkSchedule.scanLeft(0)(_+_))

      // Buffer size calculation

      // TODO: update this in the algorithm
      val l1 = srcAccBufferSum.slice(1, srcAccBufferSum.length) ::: List(srcAccBufferSum(srcAccBufferSum.length-1))
      val l2 = snkAccBufferSum
      val chBuffer =  (l1 , l2).zipped.map(_ - _)
      val bufferSize : Int = if(chBuffer.length == 0) 1 else chBuffer.reduceLeft(_ max _)

      // Enabble this for Code Genenerator Version 0
      val snkLastStepSize = ii - (snkSchedulePeriod * snkRepetition) + snkSchedulePeriod
      val iterationGap = snkLastStepSize - snkSchedulePeriod
      val snkReadEnCtrl = snkSchedule.toList.slice(1, snkSchedule.length) ::: List.fill(iterationGap)(0)
      //val snkReadEnCtrl = List(0)

      ///////////////////////////////////////////////////////////////////
      FifoProps(ii, snkAp mkString, srcSchedulePeriod, snkSchedulePeriod, snkStartSchedule, shortedPredecessorSP, bufferSize, srcAp, snkAp, snkReadEnCtrl)
    }

    def bufferSlots(thr: Double): Map[String, FifoProps] = {
      val roots = getRootNodes
      val reps = repetition
      val ii = period(thr)
      var slots = scala.collection.immutable.Map[String, FifoProps]()

      // traverse the actors
      getOrderedNodes /*g.nodes*/ map { n =>

        /** map the incoming channel of each actor */

        n.incoming.map { e =>

          // val prevSnkSchedulingPeriod = if (roots.contains(e.source)) 0 else slots.get(e.source.incoming.head.id).get
          //   .snkSchedulingPeriod
          val prevSnkSchedulingPeriod =
            if (roots.contains(e.source)) 0
            else {
              if (slots.get(e.source.incoming.head.id) != None)
                slots.get(e.source.incoming.head.id).get.snkSchedulingPeriod
              else
                0
            }

          val shortedPredecessorSP = if (roots.contains(e.source)) 0
          else if (e.target.execTime == 1) slots.get(e.source.incoming.head.id).get.srcSchedulingPeriod
          else if (e.source.execTime == 1 && slots.get(e.source.incoming.head.id).get.shortedPredecessorSP > 0) slots
            .get(e.source.incoming.head.id).get.shortedPredecessorSP
          else 0
          val buffer = channelBuffer(e.source,
            e.target,
            e.srcPort,
            e.snkPort,
            reps.get(e.source).get,
            reps.get(e.target).get,
            ii,
            prevSnkSchedulingPeriod,
            shortedPredecessorSP)

          slots = slots + (e.id -> buffer)
        }
      }

      slots
    }

    def period(thr: Double): Int = {
      val roots = getRootNodes
      val end = getEndNodes.head
      val endEdge = getPassEndEdge.head
      val reps = repetition
      val ii = Math.ceil((reps.get(end).get * endEdge.snkPort.rate).toDouble / thr.toDouble).toInt
      ii
    }

    def latency(slots: Map[String, FifoProps]): Int = {
      val reps = repetition
      val root = getRootNodes.head
      val end = getEndNodes.head
      val rootToEndPath = root pathTo end
      val edges = rootToEndPath.map(_.edges).get
      val snkStartDelays = edges.map { e => (slots.get(e.id).get.snkStartSchedule + 1) }

      snkStartDelays.reduceLeft(_ + _) + (((reps.get(end).get - 1) * slots.get(edges.head.id).get
        .snkSchedulingPeriod)) + end.execTime - 1
    }

    def repetition: Map[Actor, Int] = {

      def rootRepetition(r: g.NodeT): Int = {
        var reps = (g nodes).map { n => (n -> 0) }.toMap
        var rRepetition = reps(r)
        if (reps(r) == 0) {
          rRepetition = if (!r.incoming.isEmpty) {
            val rootInE = r.incoming.head;
            if (rootInE.dly > 0) (rootInE.dly / rootInE.snkPort.rate) else rootInE.srcPort.rate
          } else if (!r.outgoing.isEmpty) {
            val rootOutE = r.outgoing.head;
            rootOutE.snkPort.rate
          } else 1
        } else {
          r.edges foreach {
            e => {
              val prdTokens = (e.srcPort.rate * reps(e.srcActor))
              val cnsTokens = (e.snkPort.rate * reps(e.snkActor))
              if (prdTokens != cnsTokens) {
                if (r.outgoing.contains(e)) rRepetition = cnsTokens else rRepetition = prdTokens
              }
            }
          }
        }

        rRepetition
      }

      // create a topology matrix of the SDF graph
      val topology = topologyM
      // determine the matrix rank
      val rank = topologyM rank
      // determine the firing vector
      val root = getRootNodes

      var repetionMap: Map[Actor, Int] = null
      var checkValid: Boolean = false;
      var count = 1


      //println("rank " + rank)
      //println("g.order  " + g.order )
      // verify if the matrix rank is on less than the graph order
      if (rank == (g.order - 1)) {
        while (!checkValid && count < 100000) {

          val rootRep = count * rootRepetition(root.head)
          val firingMap = repetitionM(rootRep)
          val firingVector = repetitionV(firingMap)

          repetionMap = firingMap

          /* println("-------------------")
          println("Topology Matrix")
          println(topology)
          println("-------------------")
          print("Matrix Rank: "); println(rank)
          println("-------------------")
          println("Firing Vector")
          firingMap.foreach{ e => println("[" + e._1.id + "] = " + e._2)} */
          //    println("-------------------")

          // check if the [topology] x [firingVector] = 0
          val prod = topology * firingVector
          /*println("[Topology]x[Firing Vector]")
          println(prod)
          println("-------------------")*/
          val it = ((prod flatten) grouped (prod.rows))
          if ((it.next.count {
            _ == 0
          } == prod.rows)) {
            // fire each actor until a count is reached as specified in firing vector
            if (isRepetitionValid(firingMap))
              checkValid = true
          } else {
            checkValid = false
          }
          count = count + 1
        }
      }
      repetionMap
    }

    def isConsistent: Boolean = {
      val repMap = repetition
      // create a topology matrix of the SDF graph
      val topology = topologyM


      // return the repetition vector  = [size(m) x 1 matrix]
      val firingVector = repetitionV(repMap)

      val prod = topology * firingVector

      val it = ((prod flatten) grouped (prod.rows))
      if (it.next.count {
        _ == 0
      } == prod.rows)
        true
      else
        false
    }

    private def computeRepetitions(rootRep: Int): Map[g.NodeT, Int] = {

      // greatest common divisor
      def gcd(a: Int, b: Int): Int = if (b == 0) a.abs else gcd(b, a % b)

      // initialize a map of actors and firing frequency=0
      var reps = (g nodes).map { n => (n -> 0) }.toMap

      def setReps(A: g.NodeT, n: Int, p: String) {
        reps = reps + (A -> n)
        for (o <- A.outgoing) {
          if (reps(o.snkActor) == 0 && !p.equalsIgnoreCase(o.snkActor.inst)) {
            val cd = gcd(n * o.srcPort.rate, o.snkPort.rate)
            setReps(o.snkActor, ((n * o.srcPort.rate) / cd) / (o.snkPort.rate / cd), o.srcActor.inst)
          }
        }

        for (i <- A.incoming) {
          if (reps(i.srcActor) == 0 && !p.equalsIgnoreCase(i.srcActor.inst)) {
            val cd = gcd(n * i.snkPort.rate, i.srcPort.rate)
            setReps(i.srcActor, ((n * i.snkPort.rate) / cd) / (i.srcPort.rate / cd), i.snkActor.inst)
          }
        }
      }

      val root = getRootNodes
      val end = getEndNodes
      var rootEdgeCount = 0
      var repetitionTemp = -1
      var count = 1;

      // val actors = List(root) ::: g.nodes.toList.filterNot { _ == root }
      val actors = List(root.head, end.head)
      for (a <- actors) {
        val zeroExists = reps.map(_._2).toList.contains(0)
        if ( /*rootEdgeCount < a.edges.size && */ zeroExists) {
          reps.foreach { case (k, v) => reps = reps + (k -> 0) }
          setReps(a, rootRep, "")
          count = count + 1
          rootEdgeCount = rootEdgeCount + 1
        }
      }

      // get the list of firings
      val rates = (reps.toList map { case (k, v) => v }).toList
      // find the GCD of the list
      val cd = rates.reduceLeft(gcd(_, _))
      // divide firing map by GCD
      for ((k, v) <- reps) {
        reps = reps + (k -> v / cd)
      }

      reps
    }

    def drawGraph: Unit = {

      val rootDotGraph = new DotRootGraph(directed = true, id = Some("Dot"))

      def edgeTransformer(innerEdge: Graph[Actor, Channel]#EdgeT): Option[(DotGraph, DotEdgeStmt)] = {
        val edge = innerEdge.edge
        val label = "[" + edge.srcPort.rate + "," + edge.snkPort.rate + "," + edge.dly + "]"
        //val fromNode = edge.from.nodeName
        Some(rootDotGraph,
          DotEdgeStmt(NodeId(edge.from.toNodeName),
            NodeId(edge.to.toNodeName),
            if (label.nonEmpty) List(DotAttr(Id("label"), Id(label)))
            else Nil))
      }

      val dot = g.toDot(rootDotGraph, edgeTransformer(_))
      val dotFile = new PrintWriter("out/App.dot")
      dotFile.println(dot.toString)
      dotFile.close
      "dot -Tpng out/App.dot -o out/App.png" !
    }


    /**
      * Find the indices of edges between common source and target actor
      */
    def commonEdgesIndices(): Map[Channel[Actor], Int] = {

      val l = getOrderedNodes.map { n =>
        (n.incoming.toList.indices zip n.incoming.toList).map { e =>
          val src = Actor(e._2.source.id, e._2.source.inst, e._2.source.params, e._2.source.execTime)
          val snk = Actor(e._2.target.id, e._2.target.inst, e._2.target.params, e._2.target.execTime)
          val ch = Channel[Actor](src, snk, e._2.id, e._2.srcPort, e._2.snkPort, e._2.dly, e._2.portMap)
          (ch, e._1)
        }.toMap
      }
      l.flatten.toMap

    }


    /**
      * Find the count of edges between common source and target actor
      */
    def commonEdgesCount(): Map[(Actor, Actor), Int] = {
      var l = ListBuffer[(Actor, Actor)]()
      g.edges.foreach { e => l += ((e.source, e.target)) }
      l.groupBy(identity).mapValues(_.size)
    }

    def order = g.order

    def size = g.size
  }


  implicit class ExtGraphNode[N, E[X] <: EdgeLikeIn[X]](node_ : Graph[Actor, Channel]#NodeT) {
    type NodeT = graph.NodeT
    val graph = node_.containingGraph
    val node = node_.asInstanceOf[NodeT]
    val graphSize = graph.graphSize

    def toNodeName = node.id + ":" + node.inst
  }

}
