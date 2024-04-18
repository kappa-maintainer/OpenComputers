package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT.*
import net.minecraft.item.ItemMap
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraft.world.storage.MapData

class NavigationUpgradeData extends ItemData(Constants.ItemName.NavigationUpgrade) {
  def this(stack: ItemStack) = {
    this()
    load(stack)
  }

  var map = new ItemStack(net.minecraft.init.Items.FILLED_MAP)

  def mapData(world: World): MapData = try map.getItem.asInstanceOf[ItemMap].getMapData(map, world) catch
    case _: Throwable => throw new Exception("invalid map")


  def getSize(world: World): Int = {
    val info = mapData(world)
    128 * (1 << info.scale)
  }

  private final val DataTag = Settings.namespace + "data"
  private final val MapTag = Settings.namespace + "map"

  override def load(stack: ItemStack):Unit = {
    if (stack.hasTagCompound) {
      load(stack.getTagCompound.getCompoundTag(DataTag))
    }
  }

  override def save(stack: ItemStack):Unit = {
    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound())
    }
    save(stack.getCompoundTag(DataTag))
  }

  override def load(nbt: NBTTagCompound):Unit = {
    if (nbt.hasKey(MapTag)) {
      map = new ItemStack(nbt.getCompoundTag(MapTag))
    }
  }

  override def save(nbt: NBTTagCompound):Unit = {
    if (map != null) {
      nbt.setNewCompoundTag(MapTag, map.writeToNBT)
    }
  }
}
