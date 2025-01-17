package li.cil.oc.server.component

import java.io._
import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.network.ComponentConnector
import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.nbt.NBTTagCompound

import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions

abstract class WirelessNetworkCard(host: EnvironmentHost) extends NetworkCard(host) with WirelessEndpoint {
  override val node: ComponentConnector = Network.newNode(this, Visibility.Network).
    withComponent("modem", Visibility.Neighbors).
    withConnector().
    create()

  protected def wirelessCostPerRange: Double

  protected def maxWirelessRange: Double

  protected def shouldSendWiredTraffic: Boolean

  var strength = maxWirelessRange

  def position = BlockPosition(host)

  override def x = position.x

  override def y = position.y

  override def z = position.z

  override def world = host.world

  def receivePacket(packet: Packet, source: WirelessEndpoint):Unit = {
    val (dx, dy, dz) = ((source.x + 0.5) - host.xPosition, (source.y + 0.5) - host.yPosition, (source.z + 0.5) - host.zPosition)
    val distance = Math.sqrt(dx * dx + dy * dy + dz * dz)
    receivePacket(packet, distance, host)
  }

  // ----------------------------------------------------------------------- //
  
  @Callback(direct = true, doc = """function():number -- Get the signal strength (range) used when sending messages.""")
  def getStrength(context: Context, args: Arguments): Array[AnyRef] = result(strength)

  @Callback(doc = """function(strength:number):number -- Set the signal strength (range) used when sending messages.""")
  def setStrength(context: Context, args: Arguments): Array[AnyRef] = {
    strength = math.max(0, math.min(args.checkDouble(0), maxWirelessRange))
    result(strength)
  }

  override def isWireless(context: Context, args: Arguments): Array[AnyRef] = result(true)
  
  override def isWired(context: Context, args: Arguments): Array[AnyRef] = result(shouldSendWiredTraffic)
  
  override protected def doSend(packet: Packet):Unit = {
    if (strength > 0) {
      checkPower()
      api.Network.sendWirelessPacket(this, strength, packet)
    }
    if (shouldSendWiredTraffic)
      super.doSend(packet)
  }

  override protected def doBroadcast(packet: Packet):Unit = {
    if (strength > 0) {
      checkPower()
      api.Network.sendWirelessPacket(this, strength, packet)
    }
    if (shouldSendWiredTraffic)
      super.doBroadcast(packet)
  }
  
  private def checkPower():Unit = {
    val cost = wirelessCostPerRange
    if (cost > 0 && !Settings.get.ignorePower) {
      if (!node.tryChangeBuffer(-strength * cost)) {
        throw new IOException("not enough energy")
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update():Unit = {
    super.update()
    if (world.getTotalWorldTime % 20 == 0) {
      api.Network.updateWirelessNetwork(this)
    }
  }

  override def onConnect(node: Node):Unit = {
    super.onConnect(node)
    if (node == this.node) {
      api.Network.joinWirelessNetwork(this)
    }
  }

  override def onDisconnect(node: Node):Unit = {
    super.onDisconnect(node)
    if (node == this.node || !world.isBlockLoaded(position)) {
      api.Network.leaveWirelessNetwork(this)
    }
  }

  // ----------------------------------------------------------------------- //

  private final val StrengthTag = "strength"

  override def load(nbt: NBTTagCompound):Unit = {
    super.load(nbt)
    if (nbt.hasKey(StrengthTag)) {
      strength = nbt.getDouble(StrengthTag) max 0 min maxWirelessRange
    }
  }

  override def save(nbt: NBTTagCompound):Unit = {
    super.save(nbt)
    nbt.setDouble(StrengthTag, strength)
  }
}

object WirelessNetworkCard {
  class Tier1(host: EnvironmentHost) extends WirelessNetworkCard(host) {
    override protected def wirelessCostPerRange: Double = Settings.get.wirelessCostPerRange(Tier.One)
    
    override protected def maxWirelessRange: Double = Settings.get.maxWirelessRange(Tier.One)
    
    // wired network card is before wireless cards in max port list
    override protected def maxOpenPorts: Int = Settings.get.maxOpenPorts(Tier.One + 1)
    
    override protected def shouldSendWiredTraffic = false

    // ----------------------------------------------------------------------- //

    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Network,
      DeviceAttribute.Description -> "Wireless ethernet controller",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "39i110 (LPPW-01)",
      DeviceAttribute.Version -> "1.0",
      DeviceAttribute.Capacity -> Settings.get.maxNetworkPacketSize.toString,
      DeviceAttribute.Size -> maxOpenPorts.toString,
      DeviceAttribute.Width -> maxWirelessRange.toString
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo.asJava

    override protected def isPacketAccepted(packet: Packet, distance: Double): Boolean = {
      if (distance <= maxWirelessRange && (distance > 0 || shouldSendWiredTraffic)) {
        super.isPacketAccepted(packet, distance)
      } else {
        false
      }
    }
  }

  class Tier2(host: EnvironmentHost) extends Tier1(host) {
    override protected def wirelessCostPerRange: Double = Settings.get.wirelessCostPerRange(Tier.Two)
    
    override protected def maxWirelessRange: Double = Settings.get.maxWirelessRange(Tier.Two)
    
    // wired network card is before wireless cards in max port list
    override protected def maxOpenPorts: Int = Settings.get.maxOpenPorts(Tier.Two + 1)
    
    override protected def shouldSendWiredTraffic = true

    // ----------------------------------------------------------------------- //

    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Network,
      DeviceAttribute.Description -> "Wireless ethernet controller",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "62i230 (MPW-01)",
      DeviceAttribute.Version -> "2.0",
      DeviceAttribute.Capacity -> Settings.get.maxNetworkPacketSize.toString,
      DeviceAttribute.Size -> maxOpenPorts.toString,
      DeviceAttribute.Width -> maxWirelessRange.toString
    )
    
    override def getDeviceInfo: util.Map[String, String] = deviceInfo.asJava
  }
}
