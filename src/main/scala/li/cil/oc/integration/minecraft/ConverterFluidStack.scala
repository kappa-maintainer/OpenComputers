package li.cil.oc.integration.minecraft

import java.util

import li.cil.oc.api

import scala.jdk.CollectionConverters.*

object ConverterFluidStack extends api.driver.Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: net.minecraftforge.fluids.FluidStack =>
        output.asScala += "amount" -> Int.box(stack.amount)
        output.asScala += "hasTag" -> Boolean.box(stack.tag != null)
        val fluid = stack.getFluid
        if (fluid != null) {
          output.asScala += "name" -> fluid.getName
          output.asScala += "label" -> fluid.getLocalizedName(stack)
        }
      case _ =>
    }
}
