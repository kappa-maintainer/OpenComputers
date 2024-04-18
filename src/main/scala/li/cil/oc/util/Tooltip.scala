package li.cil.oc.util

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.client.KeyBindings
import net.minecraft.client.Minecraft

import scala.jdk.CollectionConverters.*

object Tooltip {
  private val maxWidth = 220

  private def font = Minecraft.getMinecraft.fontRenderer

  def get(name: String, args: Any*): java.util.List[String] = {
    if (!Localization.canLocalize(Settings.namespace + "tooltip." + name)) return Seq.empty[String].asJava
    val tooltip = Localization.localizeImmediately("tooltip." + name).
      format(args.map(_.toString)*)
    if (font == null) return tooltip.lines.toList // Some mods request tooltips before font renderer is available.
    val isSubTooltip = name.contains(".")
    val shouldShorten = (isSubTooltip || font.getStringWidth(tooltip) > maxWidth) && !KeyBindings.showExtendedTooltips
    if (shouldShorten) {
      if (isSubTooltip) Seq.empty[String].asJava
      else Seq(Localization.localizeImmediately("tooltip.toolong", KeyBindings.getKeyBindingName(KeyBindings.extendedTooltip))).asJava
    }
    else tooltip.
      lines.
      toList.
      asScala.flatMap(font.listFormattedStringToWidth(_, maxWidth).asScala.map(_.trim() + " ")).
      toList.
      asJava
  }

  def extended(name: String, args: Any*): java.util.List[String] =
    if (KeyBindings.showExtendedTooltips) {
      Localization.localizeImmediately("tooltip." + name).
        format(args.map(_.toString)*).
        lines.
        toList.
        asScala.flatMap(font.listFormattedStringToWidth(_, maxWidth).asScala.map(_.trim() + " ")).
        toList.
        asJava
    }
    else Seq.empty[String].asJava
}
