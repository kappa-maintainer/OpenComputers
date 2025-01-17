package li.cil.oc.common.tileentity

import java.util
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.Driver
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.internal
import li.cil.oc.api.network.Connector
import li.cil.oc.common
import li.cil.oc.common.InventorySlots
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.block.property.PropertyRunning
import li.cil.oc.util.Color
import net.minecraft.block.properties.PropertyBool
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.jdk.CollectionConverters.*

class Case(var tier: Int) extends traits.PowerAcceptor with traits.Computer with traits.Colored with internal.Case with DeviceInfo {
  def this() = {
    this(0)
    // If no tier was defined when constructing this case, then we don't yet know the inventory size
    // this is set back to true when the nbt data is loaded
    isSizeInventoryReady = false
  }

  // Used on client side to check whether to render disk activity/network indicators.
  var lastFileSystemAccess = 0L
  var lastNetworkActivity = 0L

  setColor(Color.rgbValues(Color.byTier(tier)))

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.System,
    DeviceAttribute.Description -> "Computer",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Blocker",
    DeviceAttribute.Capacity -> getSizeInventory.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo.asJava

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: EnumFacing) = side != facing

  override protected def connector(side: EnumFacing) = Option(if (side != facing && machine != null) machine.node.asInstanceOf[Connector] else null)

  override def energyThroughput = Settings.get.caseRate(tier)

  def isCreative = tier == Tier.Four

  // ----------------------------------------------------------------------- //

  override def componentSlot(address: String) = components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  // ----------------------------------------------------------------------- //

  override def updateEntity():Unit ={
    if (isServer && isCreative && getWorld.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      // Creative case, make it generate power.
      node.asInstanceOf[Connector].changeBuffer(Double.PositiveInfinity)
    }
    super.updateEntity()
  }

  // ----------------------------------------------------------------------- //

  override protected def onRunningChanged(): Unit = {
    super.onRunningChanged()
    getBlockType match {
      case block: common.block.Case => {
        val state = getWorld.getBlockState(getPos)
        // race condition that the world no longer has this block at the position (e.g. it was broken)
        if (block == state.getBlock) {
          getWorld.setBlockState(getPos, state.withProperty(PropertyRunning.Running.asInstanceOf[PropertyBool], Boolean.box(isRunning)))
        }
      }
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  private final val TierTag = Settings.namespace + "tier"

  override def readFromNBTForServer(nbt: NBTTagCompound):Unit ={
    tier = nbt.getByte(TierTag) max 0 min 3
    setColor(Color.rgbValues(Color.byTier(tier)))
    super.readFromNBTForServer(nbt)
    isSizeInventoryReady = true
  }

  override def writeToNBTForServer(nbt: NBTTagCompound):Unit ={
    nbt.setByte(TierTag, tier.toByte)
    super.writeToNBTForServer(nbt)
  }

  // ----------------------------------------------------------------------- //

  override protected def onItemAdded(slot: Int, stack: ItemStack):Unit ={
    super.onItemAdded(slot, stack)
    if (isServer) {
      if (InventorySlots.computer(tier)(slot).slot == Slot.Floppy) {
        common.Sound.playDiskInsert(this)
      }
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack):Unit ={
    super.onItemRemoved(slot, stack)
    if (isServer) {
      val slotType = InventorySlots.computer(tier)(slot).slot
      if (slotType == Slot.Floppy) {
        common.Sound.playDiskEject(this)
      }
      if (slotType == Slot.CPU) {
        machine.stop()
      }
    }
  }

  override def getSizeInventory = if (tier < 0 || tier >= InventorySlots.computer.length) 0 else InventorySlots.computer(tier).length

  override def isUsableByPlayer(player: EntityPlayer) =
    super.isUsableByPlayer(player) && (!isCreative || player.capabilities.isCreativeMode)

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    Option(Driver.driverFor(stack, getClass)).fold(false)(driver => {
      val provided = InventorySlots.computer(tier)(slot)
      driver.slot(stack) == provided.slot && driver.tier(stack) <= provided.tier
    })
}
