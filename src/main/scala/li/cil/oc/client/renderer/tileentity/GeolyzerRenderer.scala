package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Geolyzer
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11

object GeolyzerRenderer extends TileEntitySpecialRenderer[Geolyzer] {
  override def render(geolyzer: Geolyzer, x: Double, y: Double, z: Double, f: Float, damage: Int, alpha: Float):Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    RenderState.pushAttrib()

    RenderState.disableEntityLighting()
    RenderState.makeItBlend()
    RenderState.setBlendAlpha(1)
    GlStateManager.color(1, 1, 1, 1)

    GlStateManager.pushMatrix()

    GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5)
    GlStateManager.scale(1.0025, -1.0025, 1.0025)
    GlStateManager.translate(-0.5f, -0.5f, -0.5f)

    val t = Tessellator.getInstance
    val r = t.getBuffer

    Textures.Block.bind()
    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

    val icon = Textures.getSprite(Textures.Block.GeolyzerTopOn)
    r.pos(0, 0, 1).tex(icon.getMinU, icon.getMaxV).endVertex()
    r.pos(1, 0, 1).tex(icon.getMaxU, icon.getMaxV).endVertex()
    r.pos(1, 0, 0).tex(icon.getMaxU, icon.getMinV).endVertex()
    r.pos(0, 0, 0).tex(icon.getMinU, icon.getMinV).endVertex()

    t.draw()

    RenderState.disableBlend()
    RenderState.enableEntityLighting()

    GlStateManager.popMatrix()
    RenderState.popAttrib()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

}
