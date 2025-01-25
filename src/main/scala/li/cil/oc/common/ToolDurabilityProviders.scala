package li.cil.oc.common

import java.lang.reflect.Method
import net.minecraft.item.ItemStack

import scala.collection.mutable
import scala.util.boundary
import scala.util.boundary.break

object ToolDurabilityProviders {
  private val providers = mutable.ArrayBuffer.empty[Method]

  def add(provider: Method): Unit = providers += provider

  def getDurability(stack: ItemStack): Option[Double] = {
    boundary:
      for (provider <- providers) {
        val durability = IMC.tryInvokeStatic(provider, stack)(Double.NaN)
        if (!durability.isNaN) break(Option(durability))
      }
    // Fall back to vanilla damage values.
    if (stack.isItemStackDamageable) Option(1.0 - stack.getItemDamage.toDouble / stack.getMaxDamage.toDouble)
    else None
  }
}
