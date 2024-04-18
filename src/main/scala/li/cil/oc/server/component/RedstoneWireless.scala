package li.cil.oc.server.component

import codechicken.lib.vec.Vector3
import codechicken.wirelessredstone.api.WirelessReceivingDevice
import codechicken.wirelessredstone.api.WirelessTransmittingDevice
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.common.EventHandler
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.Optional

import scala.jdk.CollectionConverters.*

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "codechicken.wirelessredstone.api.WirelessReceivingDevice", modid = Mods.IDs.WirelessRedstoneCBE),
  new Optional.Interface(iface = "codechicken.wirelessredstone.api.WirelessTransmittingDevice", modid = Mods.IDs.WirelessRedstoneCBE)
))
trait RedstoneWireless extends RedstoneSignaller with WirelessReceivingDevice with WirelessTransmittingDevice with DeviceInfo {
  def redstone: EnvironmentHost

  var wirelessFrequency = 0

  var wirelessInput = false

  var wirelessOutput = false

  // ----------------------------------------------------------------------- //

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Communication,
    DeviceAttribute.Description -> "Wireless redstone controller",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Rw400-M",
    DeviceAttribute.Capacity -> "1",
    DeviceAttribute.Width -> "1"
  )

  override def getDeviceInfo: java.util.Map[String, String] = deviceInfo.asJava

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():number -- Get the wireless redstone input.""")
  def getWirelessInput(context: Context, args: Arguments): Array[AnyRef] = {
    wirelessInput = util.WirelessRedstone.getInput(this)
    result(wirelessInput)
  }

  @Callback(direct = true, doc = """function():boolean -- Get the wireless redstone output.""")
  def getWirelessOutput(context: Context, args: Arguments): Array[AnyRef] = result(wirelessOutput)

  @Callback(doc = """function(value:boolean):boolean -- Set the wireless redstone output.""")
  def setWirelessOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val oldValue = wirelessOutput
    val newValue = args.checkBoolean(0)

    if (oldValue != newValue) {
      wirelessOutput = newValue

      util.WirelessRedstone.updateOutput(this)

      if (Settings.get.redstoneDelay > 0)
        context.pause(Settings.get.redstoneDelay)
    }

    result(oldValue)
  }

  @Callback(direct = true, doc = """function():number -- Get the currently set wireless redstone frequency.""")
  def getWirelessFrequency(context: Context, args: Arguments): Array[AnyRef] = result(wirelessFrequency)

  @Callback(doc = """function(frequency:number):number -- Set the wireless redstone frequency to use.""")
  def setWirelessFrequency(context: Context, args: Arguments): Array[AnyRef] = {
    val oldValue = wirelessFrequency
    val newValue = args.checkInteger(0)

    if (oldValue != newValue) {
      util.WirelessRedstone.removeReceiver(this)
      util.WirelessRedstone.removeTransmitter(this)

      wirelessFrequency = newValue
      wirelessInput = false
      wirelessOutput = false

      util.WirelessRedstone.addReceiver(this)

      context.pause(0.5)
    }

    result(oldValue)
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.WirelessRedstoneCBE)
  override def updateDevice(frequency: Int, on: Boolean):Unit = {
    if (frequency == wirelessFrequency && on != wirelessInput) {
      wirelessInput = on
      onRedstoneChanged(RedstoneChangedEventArgs(null, if (on) 0 else 1, if (on) 1 else 0))
    }
  }

  @Optional.Method(modid = Mods.IDs.WirelessRedstoneCBE)
  override def getTransmitPos = new Vector3(redstone.xPosition, redstone.yPosition, redstone.zPosition)

  @Optional.Method(modid = Mods.IDs.WirelessRedstoneCBE)
  override def getDimension: Int = redstone.world.provider.getDimension

  @Optional.Method(modid = Mods.IDs.WirelessRedstoneCBE)
  override def getFreq: Int = wirelessFrequency

  @Optional.Method(modid = Mods.IDs.WirelessRedstoneCBE)
  override def getAttachedEntity = null

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node):Unit = {
    super.onConnect(node)
    if (node == this.node) {
      EventHandler.scheduleWirelessRedstone(this)
    }
  }

  override def onDisconnect(node: Node):Unit = {
    super.onDisconnect(node)
    if (node == this.node) {
      util.WirelessRedstone.removeReceiver(this)
      util.WirelessRedstone.removeTransmitter(this)
      wirelessOutput = false
      wirelessFrequency = 0
    }
  }

  // ----------------------------------------------------------------------- //

  private final val WirelessFrequencyTag = "wirelessFrequency"
  private final val WirelessInputTag = "wirelessInput"
  private final val WirelessOutputTag = "wirelessOutput"

  override def load(nbt: NBTTagCompound):Unit = {
    super.load(nbt)
    wirelessFrequency = nbt.getInteger(WirelessFrequencyTag)
    wirelessInput = nbt.getBoolean(WirelessInputTag)
    wirelessOutput = nbt.getBoolean(WirelessOutputTag)
  }

  override def save(nbt: NBTTagCompound):Unit = {
    super.save(nbt)
    nbt.setInteger(WirelessFrequencyTag, wirelessFrequency)
    nbt.setBoolean(WirelessInputTag, wirelessInput)
    nbt.setBoolean(WirelessOutputTag, wirelessOutput)
  }
}
