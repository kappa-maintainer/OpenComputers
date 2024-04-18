package li.cil.oc.common.block.property

import net.minecraft.block.properties.{IProperty, PropertyBool}

object PropertyRunning {
  final val Running: IProperty[Nothing] = PropertyBool.create("running").asInstanceOf[IProperty[Nothing]]
}
