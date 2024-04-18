package li.cil.oc.common.launch

import java.util

import li.cil.oc.common.asm.ClassTransformer
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.{SortingIndex, TransformerExclusions}

@SortingIndex(1001)
@TransformerExclusions(Array("li.cil.oc.common.asm"))
class TransformerLoader extends IFMLLoadingPlugin {
  val instance: TransformerLoader = this

  override def getModContainerClass = "li.cil.oc.common.launch.CoreModContainer"

  override def getASMTransformerClass: Array[String] = Array(classOf[ClassTransformer].getName)

  override def getAccessTransformerClass: String = null

  override def getSetupClass: String = null

  override def injectData(data: util.Map[String, AnyRef]):Unit = {}
}
