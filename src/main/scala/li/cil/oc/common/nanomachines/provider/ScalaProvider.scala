package li.cil.oc.common.nanomachines.provider

import li.cil.oc.api.nanomachines.Behavior
import li.cil.oc.api.prefab.AbstractProvider
import net.minecraft.entity.player.EntityPlayer

import scala.jdk.CollectionConverters.*

abstract class ScalaProvider(id: String) extends AbstractProvider(id) {
  def createScalaBehaviors(player: EntityPlayer): Iterable[Behavior]

  override def createBehaviors(player: EntityPlayer): java.lang.Iterable[Behavior] = createScalaBehaviors(player).asJava
}
