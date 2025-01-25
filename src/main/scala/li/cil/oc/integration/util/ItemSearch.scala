package li.cil.oc.integration.util

import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption.*
import net.minecraft.client.gui.inventory.GuiContainer

import scala.collection.mutable
import scala.util.boundary
import scala.util.boundary.break

object ItemSearch {

  val focusedInput = mutable.Set.empty[() => Boolean]
  val stackFocusing = mutable.Set.empty[(GuiContainer, Int, Int) => StackOption]

  def isInputFocused: Boolean = {
    boundary:
      for (f <- focusedInput) {
        if (f()) break(true)
      }
    false
  }

  def hoveredStack(container: GuiContainer, mouseX: Int, mouseY: Int): StackOption = {
    boundary:
      for (f <- stackFocusing) {
        f(container, mouseX, mouseY).foreach(stack => break(StackOption(stack)))
      }
    EmptyStack
  }
}
