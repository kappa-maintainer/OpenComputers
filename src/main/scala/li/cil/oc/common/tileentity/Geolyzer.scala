package li.cil.oc.common.tileentity

import li.cil.oc.server.component
import net.minecraft.nbt.NBTTagCompound

class Geolyzer extends traits.Environment {
  val geolyzer = new component.Geolyzer(this)

  def node = geolyzer.node

  override def readFromNBTForServer(nbt: NBTTagCompound):Unit = {
    super.readFromNBTForServer(nbt)
    geolyzer.load(nbt)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound):Unit = {
    super.writeToNBTForServer(nbt)
    geolyzer.save(nbt)
  }
}
