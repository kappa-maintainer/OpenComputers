package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.markdown.Document
import li.cil.oc.client.renderer.markdown.segment.InteractiveSegment
import li.cil.oc.client.renderer.markdown.segment.Segment
import li.cil.oc.client.{Manual => ManualAPI}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Mouse
import scala.jdk.CollectionConverters.*



class Manual extends GuiScreen with traits.Window {
  final val documentMaxWidth = 230
  final val documentMaxHeight = 176
  final val scrollPosX = 244
  final val scrollPosY = 6
  final val scrollWidth = 6
  final val scrollHeight = 180
  final val tabPosX = -23
  final val tabPosY = 7
  final val tabWidth = 23
  final val tabHeight = 26
  final val maxTabsPerSide = 7

  override val windowWidth = 256
  override val windowHeight = 192

  override def backgroundImage = Textures.GUI.Manual

  var isDragging = false
  var document: Segment = null
  var documentHeight = 0
  var currentSegment = None: Option[InteractiveSegment]
  protected var scrollButton: ImageButton = scala.compiletime.uninitialized

  private def canScroll = maxOffset > 0

  def offset = ManualAPI.history.top.offset

  def maxOffset = documentHeight - documentMaxHeight

  def resolveLink(path: String, current: String): String =
    if (path.startsWith("/")) path
    else {
      val splitAt = current.lastIndexOf('/')
      if (splitAt >= 0) current.splitAt(splitAt)._1 + "/" + path
      else path
    }

  def refreshPage(): Unit = {
    val content = Option(api.Manual.contentFor(ManualAPI.history.top.path)).
      getOrElse(Iterable("Document not found: " + ManualAPI.history.top.path).asJava)
    document = Document.parse(content.asScala)
    documentHeight = Document.height(document, documentMaxWidth, fontRenderer)
    scrollTo(offset)
  }

  def pushPage(path: String): Unit = {
    if (path != ManualAPI.history.top.path) {
      ManualAPI.history.push(new ManualAPI.History(path))
      refreshPage()
    }
  }

  def popPage(): Unit = {
    if (ManualAPI.history.size > 1) {
      ManualAPI.history.pop()
      refreshPage()
    }
    else {
      Minecraft.getMinecraft.player.closeScreen()
    }
  }

  override def actionPerformed(button: GuiButton): Unit = {
    if (button.id >= 0 && button.id < ManualAPI.tabs.length) {
      api.Manual.navigate(ManualAPI.tabs(button.id).path)
    }
  }

  override def initGui(): Unit = {
    super.initGui()

    for ((tab, i) <- ManualAPI.tabs.zipWithIndex if i < maxTabsPerSide) {
      val x = guiLeft + tabPosX
      val y = guiTop + tabPosY + i * (tabHeight - 1)
      add(buttonList, new ImageButton(i, x, y, tabWidth, tabHeight, Textures.GUI.ManualTab))
    }

    scrollButton = new ImageButton(-1, guiLeft + scrollPosX, guiTop + scrollPosY, 6, 13, Textures.GUI.ButtonScroll)
    add(buttonList, scrollButton)

    refreshPage()
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.drawScreen(mouseX, mouseY, dt)

    scrollButton.enabled = canScroll
    scrollButton.hoverOverride = isDragging

    for ((tab, i) <- ManualAPI.tabs.zipWithIndex if i < maxTabsPerSide) {
      val button = buttonList.get(i).asInstanceOf[ImageButton]
      GlStateManager.pushMatrix()
      GlStateManager.translate(button.x.toFloat + 5, button.y.toFloat + 5, zLevel)
      tab.renderer.render()
      GlStateManager.popMatrix()
    }

    currentSegment = Document.render(document, guiLeft + 8, guiTop + 8, documentMaxWidth, documentMaxHeight, offset, fontRenderer, mouseX, mouseY)

    if (!isDragging) currentSegment match {
      case Some(segment) =>
        segment.tooltip match {
          case Some(text) if text.nonEmpty => drawHoveringText(Localization.localizeImmediately(text).lines.toList, mouseX, mouseY, fontRenderer)
          case _ =>
        }
      case _ =>
    }

    if (!isDragging) for ((tab, i) <- ManualAPI.tabs.zipWithIndex if i < maxTabsPerSide) {
      val button = buttonList.get(i).asInstanceOf[ImageButton]
      if (mouseX > button.x && mouseX < button.x + tabWidth && mouseY > button.y && mouseY < button.y + tabHeight) tab.tooltip.foreach(text => {
        drawHoveringText(Localization.localizeImmediately(text).lines.toList, mouseX, mouseY, fontRenderer)
      })
    }

    if (canScroll && (isCoordinateOverScrollBar(mouseX - guiLeft, mouseY - guiTop) || isDragging)) {
      drawHoveringText(Seq(s"${100 * offset / maxOffset}%").asJava, guiLeft + scrollPosX + scrollWidth, scrollButton.y + scrollButton.height + 1, fontRenderer)
    }
  }

  override def keyTyped(char: Char, code: Int): Unit = {
    if (code == mc.gameSettings.keyBindJump.getKeyCode) {
      popPage()
    }
    else if (code == mc.gameSettings.keyBindInventory.getKeyCode) {
      mc.player.closeScreen()
    }
    else super.keyTyped(char, code)
  }

  override def handleMouseInput(): Unit = {
    super.handleMouseInput()
    if (Mouse.hasWheel && Mouse.getEventDWheel != 0) {
      if (math.signum(Mouse.getEventDWheel) < 0) scrollDown()
      else scrollUp()
    }
  }

  override def mouseClicked(mouseX: Int, mouseY: Int, button: Int): Unit = {
    super.mouseClicked(mouseX, mouseY, button)

    if (canScroll && button == 0 && isCoordinateOverScrollBar(mouseX - guiLeft, mouseY - guiTop)) {
      isDragging = true
      scrollMouse(mouseY)
    }
    else if (button == 0) currentSegment.foreach(_.onMouseClick(mouseX, mouseY))
    else if (button == 1) popPage()
  }

  override protected def mouseClickMove(mouseX: Int, mouseY: Int, lastButtonClicked: Int, timeSinceMouseClick: Long):Unit = {
    super.mouseClickMove(mouseX, mouseY, lastButtonClicked, timeSinceMouseClick)
    if (isDragging) {
      scrollMouse(mouseY)
    }
  }

  override protected def mouseReleased(mouseX: Int, mouseY: Int, button: Int):Unit = {
    super.mouseReleased(mouseX, mouseY, button)
    if (button == 0) {
      isDragging = false
    }
  }

  private def scrollMouse(mouseY: Int):Unit = {
    scrollTo(math.round((mouseY - guiTop - scrollPosY - 6.5) * maxOffset / (scrollHeight - 13.0)).toInt)
  }

  private def scrollUp(): Unit = scrollTo(offset - Document.lineHeight(fontRenderer) * 3)

  private def scrollDown(): Unit = scrollTo(offset + Document.lineHeight(fontRenderer) * 3)

  private def scrollTo(row: Int): Unit = {
    ManualAPI.history.top.offset = math.max(0, math.min(maxOffset, row))
    val yMin = guiTop + scrollPosY
    if (maxOffset > 0) {
      scrollButton.y = yMin + (scrollHeight - 13) * offset / maxOffset
    }
    else {
      scrollButton.y = yMin
    }
  }

  private def isCoordinateOverScrollBar(x: Int, y: Int) =
    x > scrollPosX && x < scrollPosX + scrollWidth &&
      y >= scrollPosY && y < scrollPosY + scrollHeight
}
