package li.cil.oc.integration.util

import li.cil.oc.server.component.RedstoneWireless

import scala.collection.mutable

object WirelessRedstone {
  val systems = mutable.Set.empty[WirelessRedstoneSystem]

  def isAvailable: Boolean = systems.nonEmpty

  def addReceiver(rs: RedstoneWireless):Unit = {
    systems.foreach(system => try system.addReceiver(rs) catch {
      case _: Throwable => // Ignore
    })
  }

  def removeReceiver(rs: RedstoneWireless):Unit = {
    systems.foreach(system => try system.removeReceiver(rs) catch {
      case _: Throwable => // Ignore
    })
  }

  def updateOutput(rs: RedstoneWireless):Unit = {
    systems.foreach(system => try system.updateOutput(rs) catch {
      case _: Throwable => // Ignore
    })
  }

  def removeTransmitter(rs: RedstoneWireless):Unit = {
    systems.foreach(system => try system.removeTransmitter(rs) catch {
      case _: Throwable => // Ignore
    })
  }

  def getInput(rs: RedstoneWireless): Boolean = systems.exists(_.getInput(rs))

  trait WirelessRedstoneSystem {
    def addReceiver(rs: RedstoneWireless): Unit

    def removeReceiver(rs: RedstoneWireless): Unit

    def updateOutput(rs: RedstoneWireless): Unit

    def removeTransmitter(rs: RedstoneWireless): Unit

    def getInput(rs: RedstoneWireless): Boolean
  }

}
