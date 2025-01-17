package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

class TabletData extends ItemData(Constants.ItemName.Tablet) {
  def this(stack: ItemStack) = {
    this()
    load(stack)
  }

  var items = Array.fill[ItemStack](32)(ItemStack.EMPTY)
  var isRunning = false
  var energy = 0.0
  var maxEnergy = 0.0
  var tier = Tier.One
  var container = ItemStack.EMPTY

  private final val ItemsTag = Settings.namespace + "items"
  private final val SlotTag = "slot"
  private final val ItemTag = "item"
  private final val IsRunningTag = Settings.namespace + "isRunning"
  private final val EnergyTag = Settings.namespace + "energy"
  private final val MaxEnergyTag = Settings.namespace + "maxEnergy"
  private final val TierTag = Settings.namespace + "tier"
  private final val ContainerTag = Settings.namespace + "container"

  override def load(nbt: NBTTagCompound):Unit = {
    nbt.getTagList(ItemsTag, NBT.TAG_COMPOUND).foreach((slotNbt: NBTTagCompound) => {
      val slot = slotNbt.getByte(SlotTag)
      if (slot >= 0 && slot < items.length) {
        items(slot) = new ItemStack(slotNbt.getCompoundTag(ItemTag))
      }
    })
    isRunning = nbt.getBoolean(IsRunningTag)
    energy = nbt.getDouble(EnergyTag)
    maxEnergy = nbt.getDouble(MaxEnergyTag)
    tier = nbt.getInteger(TierTag)
    if (nbt.hasKey(ContainerTag)) {
      container = new ItemStack(nbt.getCompoundTag(ContainerTag))
    }
  }

  override def save(nbt: NBTTagCompound):Unit = {
    nbt.setNewTagList(ItemsTag,
      items.zipWithIndex collect {
        case (stack, slot) if !stack.isEmpty => (stack, slot)
      } map {
        case (stack, slot) =>
          val slotNbt = new NBTTagCompound()
          slotNbt.setByte(SlotTag, slot.toByte)
          slotNbt.setNewCompoundTag(ItemTag, stack.writeToNBT)
      })
    nbt.setBoolean(IsRunningTag, isRunning)
    nbt.setDouble(EnergyTag, energy)
    nbt.setDouble(MaxEnergyTag, maxEnergy)
    nbt.setInteger(TierTag, tier)
    if (!container.isEmpty) nbt.setNewCompoundTag(ContainerTag, container.writeToNBT)
  }
}
