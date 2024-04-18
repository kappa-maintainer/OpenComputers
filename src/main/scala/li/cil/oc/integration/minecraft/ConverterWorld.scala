package li.cil.oc.integration.minecraft

import java.util

import li.cil.oc.api
import net.minecraft.world

import scala.jdk.CollectionConverters.*

object ConverterWorld extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case world: world.World =>
        output.asScala += "oc:flatten" -> world.provider
      case _ =>
    }
}
