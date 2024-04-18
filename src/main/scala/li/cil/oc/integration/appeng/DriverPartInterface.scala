package li.cil.oc.integration.appeng

import appeng.api.implementations.tiles.ISegmentedInventory
import appeng.api.networking.IGridHost
import appeng.api.networking.security.IActionHost
import appeng.api.parts.{IPartHost, PartItemStack}
import appeng.api.util.AEPartLocation
import li.cil.oc.api.driver
import li.cil.oc.api.driver.{EnvironmentProvider, NamedBlock}
import li.cil.oc.api.machine.{Arguments, Callback}
import li.cil.oc.api.machine.Context
import li.cil.oc.integration.ManagedTileEntityEnvironment
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object DriverPartInterface extends driver.DriverBlock {
  override def worksWith(world: World, pos: BlockPos, side: EnumFacing): Boolean =
    world.getTileEntity(pos) match {
      case container: IPartHost => {
        EnumFacing.VALUES.map(container.getPart).filter(p => p != null).map(_.getItemStack(PartItemStack.PICK)).exists(AEUtil.isPartInterface)
      }
      case _ => false
    }

  override def createEnvironment(world: World, pos: BlockPos, side: EnumFacing): DriverPartInterface.Environment = {
    val host: IPartHost = world.getTileEntity(pos).asInstanceOf[IPartHost]
    val tile = host.asInstanceOf[TileEntity & IPartHost & ISegmentedInventory & IActionHost & IGridHost]
    val aePos: AEPartLocation = side match {
      case EnumFacing.EAST => AEPartLocation.WEST
      case EnumFacing.WEST => AEPartLocation.EAST
      case EnumFacing.NORTH => AEPartLocation.SOUTH
      case EnumFacing.SOUTH => AEPartLocation.NORTH
      case EnumFacing.UP => AEPartLocation.DOWN
      case EnumFacing.DOWN => AEPartLocation.UP
    }
    new Environment(host, tile, aePos)
  }

  final class Environment(val host: IPartHost, val tile: TileEntity & IPartHost & ISegmentedInventory & IActionHost & IGridHost, val pos: AEPartLocation)
      extends ManagedTileEntityEnvironment[IPartHost](host, "me_interface")
      with NamedBlock with PartEnvironmentBase
      with NetworkControl[TileEntity & ISegmentedInventory & IActionHost & IGridHost]
  {
    override def preferredName = "me_interface"

    override def priority = 0

    @Callback(doc = "function(side:number[, slot:number]):table -- Get the configuration of the interface pointing in the specified direction.")
    def getInterfaceConfiguration(context: Context, args: Arguments): Array[AnyRef] = getPartConfig[ISegmentedInventory](context, args)

    @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number[, size:number]]):boolean -- Configure the interface pointing in the specified direction.")
    def setInterfaceConfiguration(context: Context, args: Arguments): Array[AnyRef] = setPartConfig[ISegmentedInventory](context, args)
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[?] =
      if (AEUtil.isPartInterface(stack))
        classOf[Environment]
      else null
  }

}