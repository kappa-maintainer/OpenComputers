package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.item.data.DriveData
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.ItemStack

class Drive(playerInventory: InventoryPlayer, val driveStack: () => ItemStack) extends GuiScreen with traits.Window {
  override val windowHeight = 120

  override def backgroundImage = Textures.GUI.Drive

  protected var managedButton: ImageButton = scala.compiletime.uninitialized
  protected var unmanagedButton: ImageButton = scala.compiletime.uninitialized
  protected var lockedButton: ImageButton = scala.compiletime.uninitialized

  protected override def actionPerformed(button: GuiButton):Unit = {
    if (button.id == 0) {
      ClientPacketSender.sendDriveMode(unmanaged = false)
      DriveData.setUnmanaged(driveStack(), unmanaged = false)
    } else if (button.id == 1) {
      ClientPacketSender.sendDriveMode(unmanaged = true)
      DriveData.setUnmanaged(driveStack(), unmanaged = true)
    } else if (button.id == 2) {
      ClientPacketSender.sendDriveLock()
      DriveData.lock(driveStack(), playerInventory.player)
    }
    updateButtonStates()
  }

  def updateButtonStates(): Unit = {
    val data = new DriveData(driveStack())
    unmanagedButton.toggled = data.isUnmanaged
    managedButton.toggled = !unmanagedButton.toggled
    lockedButton.toggled = data.isLocked
    lockedButton.enabled = !data.isLocked
  }

  override def initGui(): Unit = {
    super.initGui()
    managedButton = new ImageButton(0, guiLeft + 11, guiTop + 11, 74, 18, Textures.GUI.ButtonDriveMode, text = Localization.Drive.Managed, textColor = 0x608060, canToggle = true)
    unmanagedButton = new ImageButton(1, guiLeft + 91, guiTop + 11, 74, 18, Textures.GUI.ButtonDriveMode, text = Localization.Drive.Unmanaged, textColor = 0x608060, canToggle = true)
    lockedButton = new ImageButton(2, guiLeft + 11, guiTop + windowHeight - 42, 44, 18, Textures.GUI.ButtonDriveMode, text = Localization.Drive.ReadOnlyLock, textColor = 0x608060, canToggle = true)
    add(buttonList, managedButton)
    add(buttonList, unmanagedButton)
    add(buttonList, lockedButton)
    updateButtonStates()
  }

  override def updateScreen(): Unit = {
    super.updateScreen()
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.drawScreen(mouseX, mouseY, dt)
    fontRenderer.drawSplitString(Localization.Drive.Warning, guiLeft + 11, guiTop + 37, xSize - 20, 0x404040)
    fontRenderer.drawSplitString(Localization.Drive.LockWarning, guiLeft + 61, guiTop + windowHeight - 48, xSize - 68, 0x404040)
  }
}
