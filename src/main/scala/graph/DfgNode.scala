package sdrlift.graph

import sdrlift.graph.NodeFactory.PortTypeEnum

abstract  class DfgNode {
  val inst: String
  val name: String
  val prefix: String
  val width: Int
  val level: Int
  val operator: String
  val typ: PortTypeEnum.PortTypeEnum
}
