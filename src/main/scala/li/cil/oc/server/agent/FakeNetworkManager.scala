package li.cil.oc.server.agent

import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import net.minecraft.network.EnumPacketDirection
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet

object FakeNetworkManager extends NetworkManager(EnumPacketDirection.CLIENTBOUND) {
  override def sendPacket(packetIn: Packet[?]): Unit = {}

  override def sendPacket(packetIn: Packet[?], listener: GenericFutureListener[? <: Future[? >: Void]], listeners: GenericFutureListener[? <: Future[? >: Void]]*): Unit = {}
}
