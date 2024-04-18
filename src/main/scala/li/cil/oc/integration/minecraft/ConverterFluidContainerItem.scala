package li.cil.oc.integration.vanilla

import li.cil.oc.api
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.capability.{CapabilityFluidHandler, IFluidHandlerItem}

import java.util
import scala.jdk.CollectionConverters.*

object ConverterFluidContainerItem extends api.driver.Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: ItemStack => if (stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
        stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null) match {
          case fc: IFluidHandlerItem =>
            val properties = fc.getTankProperties
            output.asScala += "capacity" -> Int.box(properties.map(a => a.getCapacity).sum)
            if (properties.length > 1) {
              output.asScala += "fluid" -> properties
            } else {
              output.asScala += "fluid" -> properties(0).getContents
            }
          case _ =>
        }
      }
      case _ =>
    }
}
