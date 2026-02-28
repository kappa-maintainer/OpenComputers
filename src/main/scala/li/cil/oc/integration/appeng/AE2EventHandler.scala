package li.cil.oc.integration.appeng

import li.cil.oc.common.EventHandler
import li.cil.oc.common.tileentity.traits.power
import li.cil.oc.util.SideTracker

object AE2EventHandler {
  def scheduleAE2Add(tileEntity: power.AppliedEnergistics2): Unit = {
    if (SideTracker.isServer) EventHandler.scheduleServer(() => tileEntity.updateGridNodeState())
  }
}
