package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.util.StateAware
import li.cil.oc.common.template.DisassemblerTemplates
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ItemUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.jdk.CollectionConverters.*
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class Disassembler extends traits.Environment with traits.PowerAcceptor with traits.Inventory with traits.StateAware with traits.PlayerInputAware with traits.Tickable with DeviceInfo {
  val node: Connector = api.Network.newNode(this, Visibility.None).
    withConnector(Settings.get.bufferConverter).
    create()

  var isActive = false

  val queue: ArrayBuffer[ItemStack] = mutable.ArrayBuffer.empty[ItemStack]

  var totalRequiredEnergy = 0.0

  override def getInventoryStackLimit: Int = 1

  var buffer = 0.0

  var disassembleNextInstantly = false

  def progress: Double = if (queue.isEmpty) 0.0 else (1 - (queue.size * Settings.get.disassemblerItemCost - buffer) / totalRequiredEnergy) * 100

  private def setActive(value: Boolean) = if (value != isActive) {
    isActive = value
    ServerPacketSender.sendDisassemblerActive(this, isActive)
    getWorld.notifyNeighborsOfStateChange(getPos, getBlockType, true)
  }

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Disassembler",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Break.3R-100"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo.asJava

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: EnumFacing): Boolean = side != EnumFacing.UP

  override protected def connector(side: EnumFacing) = Option(if (side != EnumFacing.UP) node else null)

  override def energyThroughput: Double = Settings.get.disassemblerRate

  override def getCurrentState: util.EnumSet[StateAware.State] = {
    if (isActive) util.EnumSet.of(api.util.StateAware.State.IsWorking)
    else if (queue.nonEmpty) util.EnumSet.of(api.util.StateAware.State.CanWork)
    else util.EnumSet.noneOf(classOf[api.util.StateAware.State])
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity():Unit ={
    super.updateEntity()
    if (isServer && getWorld.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      if (queue.isEmpty) {
        val instant = disassembleNextInstantly // Is reset via decrStackSize
        disassemble(decrStackSize(0, 1), instant)
        setActive(queue.nonEmpty)
      }
      else {
        if (buffer < Settings.get.disassemblerItemCost) {
          val want = Settings.get.disassemblerTickAmount
          val success = node.tryChangeBuffer(-want)
          setActive(success) // If energy is insufficient indicate it visually.
          if (success) {
            buffer += want
          }
        }
        while (buffer >= Settings.get.disassemblerItemCost && queue.nonEmpty) {
          buffer -= Settings.get.disassemblerItemCost
          val stack = queue.remove(0)
          if (disassembleNextInstantly || getWorld.rand.nextDouble >= Settings.get.disassemblerBreakChance) {
            drop(stack)
          }
        }
      }
      disassembleNextInstantly = queue.nonEmpty // If we have nothing left to do, stop being creative.
    }
  }

  def disassemble(stack: ItemStack, instant: Boolean = false):Unit ={
    // Validate the item, never trust Minecraft / other Mods on anything!
    if (isItemValidForSlot(0, stack)) {
      val ingredients = ItemUtils.getIngredients(stack)
      DisassemblerTemplates.select(stack) match {
        case Some(template) =>
          val (stacks, drops) = template.disassemble(stack, ingredients)
          stacks.foreach(queue ++= _)
          drops.foreach(_.foreach(drop))
        case _ => queue ++= ingredients
      }
      totalRequiredEnergy = queue.size * Settings.get.disassemblerItemCost
      if (instant) {
        buffer = totalRequiredEnergy
      }
    }
    else {
      drop(stack)
    }
  }

  private def drop(stack: ItemStack):Unit ={
    if (!stack.isEmpty) {
      for (side <- EnumFacing.values if stack.getCount > 0) {
        InventoryUtils.insertIntoInventoryAt(stack, BlockPosition(this).offset(side), Some(side.getOpposite))
      }
      if (stack.getCount > 0) {
        spawnStackInWorld(stack, Option(EnumFacing.UP))
      }
    }
  }

  // ----------------------------------------------------------------------- //

  private final val QueueTag = Settings.namespace + "queue"
  private final val BufferTag = Settings.namespace + "buffer"
  private final val TotalTag = Settings.namespace + "total"
  private final val IsActiveTag = Settings.namespace + "isActive"

  override def readFromNBTForServer(nbt: NBTTagCompound):Unit ={
    super.readFromNBTForServer(nbt)
    queue.clear()
    queue ++= nbt.getTagList(QueueTag, NBT.TAG_COMPOUND).
      map((tag: NBTTagCompound) => new ItemStack(tag))
    buffer = nbt.getDouble(BufferTag)
    totalRequiredEnergy = nbt.getDouble(TotalTag)
    isActive = queue.nonEmpty
  }

  override def writeToNBTForServer(nbt: NBTTagCompound):Unit ={
    super.writeToNBTForServer(nbt)
    nbt.setNewTagList(QueueTag, queue)
    nbt.setDouble(BufferTag, buffer)
    nbt.setDouble(TotalTag, totalRequiredEnergy)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound):Unit ={
    super.readFromNBTForClient(nbt)
    isActive = nbt.getBoolean(IsActiveTag)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound):Unit ={
    super.writeToNBTForClient(nbt)
    nbt.setBoolean(IsActiveTag, isActive)
  }

  // ----------------------------------------------------------------------- //

  override def getSizeInventory = 1

  override def isItemValidForSlot(i: Int, stack: ItemStack): Boolean =
    allowDisassembling(stack) &&
      (((Settings.get.disassembleAllTheThings || api.Items.get(stack) != null) && ItemUtils.getIngredients(stack).nonEmpty) ||
        DisassemblerTemplates.select(stack).isDefined)

  private def allowDisassembling(stack: ItemStack) = !stack.isEmpty && (!stack.hasTagCompound || !stack.getTagCompound.getBoolean(Settings.namespace + "undisassemblable"))

  override def setInventorySlotContents(slot: Int, stack: ItemStack): Unit = {
    super.setInventorySlotContents(slot, stack)
    if (!getWorld.isRemote) {
      disassembleNextInstantly = false
    }
  }

  override def onSetInventorySlotContents(player: EntityPlayer, slot: Int, stack: ItemStack): Unit = {
    if (!getWorld.isRemote) {
      disassembleNextInstantly = !stack.isEmpty && slot == 0 && player.capabilities.isCreativeMode
    }
  }
}
