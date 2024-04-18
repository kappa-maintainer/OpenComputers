package li.cil.oc.common.tileentity.traits

import li.cil.oc.api

trait ImmibisMicroblock extends TileEntity {
  val ImmibisMicroblocks_TransformableTileEntityMarker = null

  def ImmibisMicroblocks_isSideOpen(side: Int) = true

  def ImmibisMicroblocks_onMicroblocksChanged():Unit = {
    api.Network.joinOrCreateNetwork(this)
  }
}
