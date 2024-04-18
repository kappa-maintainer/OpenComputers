package li.cil.oc.client.gui.widget

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*

trait WidgetContainer {
  private val widgets: ArrayBuffer[Widget] = mutable.ArrayBuffer.empty[Widget]

  def addWidget[T <: Widget](widget: T): T = {
    widgets += widget
    widget.owner = this
    widget
  }

  def windowX = 0

  def windowY = 0

  def windowZ = 0f

  def drawWidgets():Unit = {
    widgets.foreach(_.draw())
  }
}
