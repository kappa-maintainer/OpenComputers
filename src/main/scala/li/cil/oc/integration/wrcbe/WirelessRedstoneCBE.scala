package li.cil.oc.integration.wrcbe

import codechicken.wirelessredstone.manager.RedstoneEther
import li.cil.oc.integration.util.WirelessRedstone.WirelessRedstoneSystem
import li.cil.oc.server.component.RedstoneWireless

import scala.language.reflectiveCalls

object WirelessRedstoneCBE extends WirelessRedstoneSystem {
  def addTransmitter(rs: RedstoneWireless):Unit = {
    if (rs.wirelessOutput && rs.wirelessFrequency > 0) {
      RedstoneEther.server.addTransmittingDevice(rs)
    }
  }

  def removeTransmitter(rs: RedstoneWireless):Unit = {
    if (rs.wirelessFrequency > 0) {
      RedstoneEther.server.removeTransmittingDevice(rs)
    }
  }

  def addReceiver(rs: RedstoneWireless):Unit = {
    RedstoneEther.server.addReceivingDevice(rs)
    if (rs.wirelessFrequency > 0) {
      rs.wirelessInput = RedstoneEther.server.isFreqOn(rs.wirelessFrequency)
    }
  }

  def removeReceiver(rs: RedstoneWireless):Unit = {
    RedstoneEther.server.removeReceivingDevice(rs)
  }

  def updateOutput(rs: RedstoneWireless):Unit = {
    if (rs.wirelessOutput) {
      addTransmitter(rs)
    }
    else {
      removeTransmitter(rs)
    }
  }

  def getInput(rs: RedstoneWireless): Boolean = rs.wirelessInput
}
