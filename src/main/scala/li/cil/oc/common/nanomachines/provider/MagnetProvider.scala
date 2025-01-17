package li.cil.oc.common.nanomachines.provider

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.prefab.AbstractBehavior
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.Vec3d

import scala.jdk.CollectionConverters.*

object MagnetProvider extends ScalaProvider("9324d5ec-71f1-41c2-b51c-406e527668fc") {
  override def createScalaBehaviors(player: EntityPlayer) = Iterable(new MagnetBehavior(player))

  override def readBehaviorFromNBT(player: EntityPlayer, nbt: NBTTagCompound) = new MagnetBehavior(player)

  class MagnetBehavior(player: EntityPlayer) extends AbstractBehavior(player) {
    override def getNameHint = "magnet"

    override def update(): Unit = {
      val world = player.getEntityWorld
      if (!world.isRemote) {
        val actualRange = Settings.get.nanomachineMagnetRange * api.Nanomachines.getController(player).getInputCount(this)
        val items = world.getEntitiesWithinAABB(classOf[EntityItem], player.getEntityBoundingBox.grow(actualRange, actualRange, actualRange))
        items.asScala.collect {
          case item: EntityItem if !item.cannotPickup && !item.getItem.isEmpty && player.inventory.mainInventory.asScala.exists(stack => stack.isEmpty || stack.getCount < stack.getMaxStackSize && stack.isItemEqual(item.getItem)) =>
            val dx = player.posX - item.posX
            val dy = player.posY - item.posY
            val dz = player.posZ - item.posZ
            val delta = new Vec3d(dx, dy, dz).normalize()
            item.addVelocity(delta.x * 0.1, delta.y * 0.1, delta.z * 0.1)
        }
      }
    }
  }

}
