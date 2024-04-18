package li.cil.oc.client.gui.widget

abstract class Widget {
  var owner: WidgetContainer = scala.compiletime.uninitialized

  def x: Int

  def y: Int

  def width: Int

  def height: Int

  def draw(): Unit
}
