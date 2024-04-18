package li.cil.oc.integration.minecraft

import java.util

import li.cil.oc.api
import net.minecraftforge.fluids

import scala.jdk.CollectionConverters.*

object ConverterFluidTankProperties extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case properties: fluids.capability.IFluidTankProperties =>
        output.asScala += "capacity" -> Int.box(properties.getCapacity)
        val fluid = properties.getContents
        if (fluid != null) {
          ConverterFluidStack.convert(fluid, output)
        }
        else output.asScala += "amount" -> Int.box(0)
      case _ =>
    }
}
