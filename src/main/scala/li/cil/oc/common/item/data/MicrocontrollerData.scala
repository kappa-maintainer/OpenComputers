package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.Tier
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

class MicrocontrollerData(itemName: String = Constants.BlockName.Microcontroller) extends ItemData(itemName) {
  def this(stack: ItemStack) = {
    this()
    load(stack)
  }

  var tier = Tier.One

  var components: Array[ItemStack] = Array[ItemStack](ItemStack.EMPTY)

  var storedEnergy = 0

  private final val TierTag = Settings.namespace + "tier"
  private final val ComponentsTag = Settings.namespace + "components"
  private final val StoredEnergyTag = Settings.namespace + "storedEnergy"

  override def load(nbt: NBTTagCompound):Unit = {
    tier = nbt.getByte(TierTag)
    components = nbt.getTagList(ComponentsTag, NBT.TAG_COMPOUND).
      toArray[NBTTagCompound].map(new ItemStack(_)).filter(!_.isEmpty)
    storedEnergy = nbt.getInteger(StoredEnergyTag)

    // Reserve slot for EEPROM if necessary, avoids having to resize the
    // components array in the MCU tile entity, which isn't possible currently.
    if (!components.exists(stack => api.Items.get(stack) == api.Items.get(Constants.ItemName.EEPROM))) {
      components :+= ItemStack.EMPTY
    }
  }

  override def save(nbt: NBTTagCompound):Unit = {
    nbt.setByte(TierTag, tier.toByte)
    nbt.setNewTagList(ComponentsTag, components.filter(!_.isEmpty).toIterable)
    nbt.setInteger(StoredEnergyTag, storedEnergy)
  }

  def copyItemStack(): ItemStack = {
    val stack = createItemStack()
    val newInfo = new MicrocontrollerData(stack)
    newInfo.save(stack)
    stack
  }
}
