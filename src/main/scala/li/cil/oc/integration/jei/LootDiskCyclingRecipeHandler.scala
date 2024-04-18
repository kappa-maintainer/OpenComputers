package li.cil.oc.integration.jei

import java.util

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.common.Loot
import li.cil.oc.common.recipe.LootDiskCyclingRecipe
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.recipe._
import net.minecraft.item.ItemStack

import scala.jdk.CollectionConverters.*

object LootDiskCyclingRecipeHandler extends IRecipeWrapperFactory[LootDiskCyclingRecipe] {
  override def getRecipeWrapper(recipe: LootDiskCyclingRecipe): IRecipeWrapper = new LootDiskCyclingRecipeWrapper(recipe)

  class LootDiskCyclingRecipeWrapper(val recipe: LootDiskCyclingRecipe) extends BlankRecipeWrapper {

    def getInputs: util.List[util.List[ItemStack]] = List(Loot.disksForCycling.asJava, List(api.Items.get(Constants.ItemName.Wrench).createItemStack(1)).asJava).asJava

    def getOutputs: util.List[ItemStack] = Loot.disksForCycling.toList.asJava

    override def getIngredients(ingredients: IIngredients): Unit = {
      ingredients.setInputLists(classOf[ItemStack], getInputs)
      ingredients.setOutputs(classOf[ItemStack], getOutputs)
    }
  }

}



