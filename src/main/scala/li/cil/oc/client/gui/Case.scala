package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.InventoryPlayer

import scala.jdk.CollectionConverters.*

class Case(playerInventory: InventoryPlayer, val computer: tileentity.Case) extends DynamicGuiContainer(new container.Case(playerInventory, computer)) {
  protected var powerButton: ImageButton = scala.compiletime.uninitialized

  protected override def actionPerformed(button: GuiButton):Unit = {
    if (button.id == 0) {
      ClientPacketSender.sendComputerPower(computer, !computer.isRunning)
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float):Unit = {
    powerButton.toggled = computer.isRunning
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui():Unit = {
    super.initGui()
    powerButton = new ImageButton(0, guiLeft + 70, guiTop + 33, 18, 18, Textures.GUI.ButtonPower, canToggle = true)
    add(buttonList, powerButton)
  }

  override protected def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int):Unit = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      Localization.localizeImmediately(computer.getName),
      8, 6, 0x404040)
    if (powerButton.isMouseOver) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.addAll(if (computer.isRunning) Localization.Computer.TurnOff.lines.toList else Localization.Computer.TurnOn.lines.toList)
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
    }
  }

  override def drawSecondaryBackgroundLayer():Unit = {
    GlStateManager.color(1, 1, 1)
    Textures.bind(Textures.GUI.Computer)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }
}
