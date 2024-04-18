package li.cil.oc.integration.mekanism.gas

import java.util

import li.cil.oc.Settings
import li.cil.oc.api

import scala.jdk.CollectionConverters.*

object ConverterGasStack extends api.driver.Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: mekanism.api.gas.GasStack =>
        if (Settings.get.insertIdsInConverters) {
          output.asScala += "id" -> Int.box(stack.getGas.getID)
        }
        output.asScala += "amount" -> Int.box(stack.amount)
        val gas = stack.getGas
        if (gas != null) {
          output.asScala += "name" -> gas.getName
          output.asScala += "label" -> gas.getLocalizedName
        }
      case _ =>
    }
}
