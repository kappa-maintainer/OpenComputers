package li.cil.oc.common.block.property

import com.google.common.base.Predicate
import com.google.common.base.Predicates
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.util.EnumFacing

import scala.jdk.CollectionConverters.*

object PropertyRotatable {
  final val Facing: PropertyDirection = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL.asInstanceOf[Predicate[EnumFacing]])
  final val Pitch: PropertyDirection = PropertyDirection.create("pitch", Predicates.in(Set(EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH).asJavaCollection))
  final val Yaw: PropertyDirection = PropertyDirection.create("yaw", EnumFacing.Plane.HORIZONTAL.asInstanceOf[Predicate[EnumFacing]])
}
