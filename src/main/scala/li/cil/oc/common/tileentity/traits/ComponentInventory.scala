package li.cil.oc.common.tileentity.traits

import li.cil.oc.api.driver.DriverItem
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.network.Node
import li.cil.oc.common.EventHandler
import li.cil.oc.common.inventory
import li.cil.oc.util.ExtendedInventory._
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.mutable

trait ComponentInventory extends Environment with Inventory with inventory.ComponentInventory {
  override def host = this

  // ----------------------------------------------------------------------- //

  // Cache changes to inventory slots on the client side to avoid recreating
  // components when we don't have to and the slots are just cleared by MC
  // temporarily.
  private lazy val pendingRemovalsActual = mutable.ArrayBuffer.fill(getSizeInventory)(EmptyStack: StackOption)
  private lazy val pendingAddsActual = mutable.ArrayBuffer.fill(getSizeInventory)(EmptyStack: StackOption)
  private var updateScheduled = false
  def pendingRemovals = {
    adjustSize(pendingRemovalsActual)
    pendingRemovalsActual
  }
  def pendingAdds = {
    adjustSize(pendingAddsActual)
    pendingAddsActual
  }

  private def adjustSize(buffer: mutable.ArrayBuffer[StackOption]): Unit = {
    val delta = buffer.length - getSizeInventory
    if (delta > 0) {
      buffer.remove(buffer.length - delta, delta)
    }
    else if (delta < 0) {
      buffer.sizeHint(getSizeInventory)
      for (i <- 0 until -delta) {
        buffer += EmptyStack
      }
    }
  }

  private def applyInventoryChanges(): Unit = {
    updateScheduled = false
    for (slot <- this.indices) {
      (pendingRemovals(slot), pendingAdds(slot)) match {
        case (SomeStack(removed), SomeStack(added)) =>
          if (!removed.isItemEqual(added) || !ItemStack.areItemStackTagsEqual(removed, added)) {
            super.onItemRemoved(slot, removed)
            super.onItemAdded(slot, added)
            markDirty()
          } // else: No change, ignore.
        case (SomeStack(removed), EmptyStack) =>
          super.onItemRemoved(slot, removed)
          markDirty()
        case (EmptyStack, SomeStack(added)) =>
          super.onItemAdded(slot, added)
          markDirty()
        case _ => // No change.
      }

      pendingRemovals(slot) = EmptyStack
      pendingAdds(slot) = EmptyStack
    }
  }

  private def scheduleInventoryChange(): Unit = {
    if (!updateScheduled) {
      updateScheduled = true
      EventHandler.scheduleClient(() => applyInventoryChanges())
    }
  }

  override protected def onItemAdded(slot: Int, stack: ItemStack): Unit = {
    if (isServer) super.onItemAdded(slot, stack)
    else {
      pendingRemovals(slot) match {
        case SomeStack(removed) if removed.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(removed, stack) =>
          // Reverted to original state.
          pendingAdds(slot) = EmptyStack
          pendingRemovals(slot) = EmptyStack
        case _ =>
          // Got a removal and an add of *something else* in the same tick.
          pendingAdds(slot) = StackOption(stack)
          scheduleInventoryChange()
      }
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack): Unit = {
    if (isServer) super.onItemRemoved(slot, stack)
    else {
      pendingAdds(slot) match {
        case SomeStack(added) =>
          // If we have a pending add and get a remove on a slot it is
          // now either empty, or the previous remove is valid again.
          pendingAdds(slot) = EmptyStack
        case _ =>
          // If we have no pending add, only the first removal can be
          // relevant (further ones should in fact be impossible).
          if (pendingRemovals(slot).isEmpty) {
            pendingRemovals(slot) = StackOption(stack)
            scheduleInventoryChange()
          }
      }
    }
  }

  override protected def save(component: ManagedEnvironment, driver: DriverItem, stack: ItemStack): Unit = {
    if (isServer) {
      super.save(component, driver, stack)
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def initialize(): Unit = {
    super.initialize()
    if (isClient) {
      connectComponents()
    }
  }

  override def dispose(): Unit = {
    super.dispose()
    if (isClient) {
      disconnectComponents()
    }
  }

  override def onConnect(node: Node):Unit = {
    super.onConnect(node)
    if (node == this.node) {
      connectComponents()
    }
  }

  override def onDisconnect(node: Node):Unit = {
    super.onDisconnect(node)
    if (node == this.node) {
      disconnectComponents()
    }
  }

  override def hasCapability(capability: Capability[?], facing: EnumFacing): Boolean = {
    val localFacing = this match {
      case rotatable: Rotatable => rotatable.toLocal(facing)
      case _ => facing
    }
    super.hasCapability(capability, facing) || components.exists {
      case Some(component: ICapabilityProvider) => component.hasCapability(capability, localFacing)
      case _ => false
    }
  }

  override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
    val localFacing = this match {
      case rotatable: Rotatable => rotatable.toLocal(facing)
      case _ => facing
    }
    (if (super.hasCapability(capability, facing)) Option(super.getCapability(capability, facing)) else None).orElse(components.collectFirst {
      case Some(component: ICapabilityProvider) if component.hasCapability(capability, localFacing) => component.getCapability(capability, localFacing)
    }).getOrElse(null.asInstanceOf[T])
  }

  override def writeToNBTForClient(nbt: NBTTagCompound):Unit = {
    connectComponents()
    super.writeToNBTForClient(nbt)
    save(nbt)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound):Unit = {
    super.readFromNBTForClient(nbt)
    load(nbt)
    connectComponents()
  }
}
