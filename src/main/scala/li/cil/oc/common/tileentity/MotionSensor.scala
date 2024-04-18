package li.cil.oc.common.tileentity

import li.cil.oc.api.network.Node
import li.cil.oc.server.component
import net.minecraft.nbt.NBTTagCompound

class MotionSensor extends traits.Environment with traits.Tickable {
  val motionSensor = new component.MotionSensor(this)

  def node: Node = motionSensor.node

  override def updateEntity():Unit = {
    super.updateEntity()
    if (isServer) {
      motionSensor.update()
    }
  }

  override def readFromNBTForServer(nbt: NBTTagCompound):Unit = {
    super.readFromNBTForServer(nbt)
    motionSensor.load(nbt)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound):Unit = {
    super.writeToNBTForServer(nbt)
    motionSensor.save(nbt)
  }
}
