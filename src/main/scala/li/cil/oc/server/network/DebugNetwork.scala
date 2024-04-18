package li.cil.oc.server.network

import li.cil.oc.api.network.Packet

import scala.collection.mutable

object DebugNetwork {
  val cards = mutable.WeakHashMap.empty[DebugNode, Unit]

  def add(card: DebugNode):Unit = {
    cards.put(card, ())
  }

  def remove(card: DebugNode):Unit = {
    cards.remove(card)
  }

  def getEndpoint(tunnel: String) = cards.keys.find(_.address == tunnel)

  trait DebugNode {
    def address: String

    def receivePacket(packet: Packet): Unit
  }
}
