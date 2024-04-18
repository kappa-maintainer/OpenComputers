package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.server.component.result
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.FluidUtils
import net.minecraftforge.fluids.capability.IFluidTankProperties

trait WorldTankAnalytics extends WorldAware with SideRestricted {
  @Callback(doc = """function(side:number [, tank:number]):number -- Get the amount of fluid in the tank on the specified side.""")
  def getTankLevel(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)

    FluidUtils.fluidHandlerAt(position.offset(facing), facing.getOpposite) match {
      case Some(handler) => args.optTankProperties(handler, 1, null) match {
        case properties: IFluidTankProperties => result(Option(properties.getContents).fold(0)(_.amount))
        case _ => result(handler.getTankProperties.map(info => Option(info.getContents).fold(0)(_.amount)).sum)
      }
      case _ => result((), "no tank")
    }
  }

  @Callback(doc = """function(side:number [, tank:number]):number -- Get the capacity of the tank on the specified side.""")
  def getTankCapacity(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    FluidUtils.fluidHandlerAt(position.offset(facing), facing.getOpposite) match {
      case Some(handler) => args.optTankProperties(handler, 1, null) match {
        case properties: IFluidTankProperties  => result(properties.getCapacity)
        case _ => result(handler.getTankProperties.map(_.getCapacity).foldLeft(0)((max, capacity) => math.max(max, capacity)))
      }
      case _ => result((), "no tank")
    }
  }

  @Callback(doc = """function(side:number [, tank:number]):table -- Get a description of the fluid in the the tank on the specified side.""")
  def getFluidInTank(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val facing = checkSideForAction(args, 0)
    FluidUtils.fluidHandlerAt(position.offset(facing), facing.getOpposite) match {
      case Some(handler) => args.optTankProperties(handler, 1, null) match {
        case properties: IFluidTankProperties  => result(properties)
        case _ => result(handler.getTankProperties)
      }
      case _ => result((), "no tank")
    }
  }
  else result((), "not enabled in config")

  @Callback(doc = """function(side:number):number -- Get the number of tanks available on the specified side.""")
  def getTankCount(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    FluidUtils.fluidHandlerAt(position.offset(facing), facing.getOpposite) match {
      case Some(handler) => handler.getTankProperties match {
        case info: Array[IFluidTankProperties] => result(info.length)
        case _ => result((), "no tank")
      }
      case _ => result((), "no tank")
    }
  }
}
