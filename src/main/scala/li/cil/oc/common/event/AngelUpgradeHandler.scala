package li.cil.oc.common.event

import li.cil.oc.api.event.RobotPlaceInAirEvent
import li.cil.oc.api.network.Node
import li.cil.oc.server.component.UpgradeAngel
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.jdk.CollectionConverters.*

object AngelUpgradeHandler {
  @SubscribeEvent
  def onPlaceInAir(e: RobotPlaceInAirEvent):Unit = {
    val machineNode = e.agent.machine.node
    e.setAllowed(machineNode.reachableNodes.asScala.exists {
      case node: Node if node.canBeReachedFrom(machineNode) =>
        node.host.isInstanceOf[UpgradeAngel]
      case _ => false
    })
  }
}
