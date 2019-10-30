package sdrlift.graph

import exp.NodeExp.Streamer
import scalax.collection.GraphPredef._
import scalax.collection.GraphEdge._


case class DfgEdge[+N](srcEl: N, snkEl: N, id: String, srcPort: Streamer, snkPort: Streamer) //links : java.util.HashMap[String, (Actor,String)]
// DiEdge should be the base of any directed custom edge. Wire is the node type.
  extends DiEdge[N](NodeProduct(srcEl, snkEl))
    /**
      * If any of the label attributes is part of the key of the edge type,
      * ExtendedKey must be mixed in. An attribute is a key if it must be considered by equals.
      * 'id' is such a key attribute because there may exist several Wires from and to the
      * same actor so we distinguished them by 'id'.
      */
    with ExtendedKey[N]
    // All edge implementations must mix in EdgeCopy.
    with EdgeCopy[DfgEdge]
    //All edge implementations must mix in OuterEdge.
    with OuterEdge[N, DfgEdge] {

  private def this(nodes: Product, id: String, srcPort: Streamer, snkPort: Streamer) {
    this(nodes.productElement(0).asInstanceOf[N],
      nodes.productElement(1).asInstanceOf[N], id, srcPort, snkPort)
  }

  // Key attributes must be added to this Seq.
  def keyAttributes = Seq(id)

  /**
    * copy will be called by Graph transparently to create an inner edge. Thus copy plays the role
    * of an inner edge factory. It must return an instance of the edge class.
    */
  override def copy[NN](newNodes: Product) = new DfgEdge[NN](newNodes, id, srcPort, snkPort)

  // Establishes the Wire edge factory shortcut ## that propagates a directed edge to Wire.
  override protected def attributesToString = s" ($id)"

  //override def hashCode = id.toInt
  override def equals(other: Any) = other match {
    case that: DfgEdge[N] => that.id == this.id
    case _ => false
  }
}

object DfgEdge {

  /**
    * Declares the `Wire` edge factory shortcut `##` which can be invoked like
    * {{{
    * val (adder, mult) = (Actor("ADDER"), Actor("MULT"))
    * adder ~> mult ## "fifo_1" // yields Wire[Actor]
    * }}}
    */
  implicit final class ImplicitEdge[A <: DfgNode](val e: DiEdge[A]) extends AnyVal {
    def ##(id: String, srcPort: Streamer, snkPort: Streamer) = new DfgEdge[A](e.source, e.target, id, srcPort, snkPort)
  }

}

