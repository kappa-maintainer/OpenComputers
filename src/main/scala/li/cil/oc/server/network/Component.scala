package li.cil.oc.server.network

import li.cil.oc.api.machine.Context
import li.cil.oc.api.network
import li.cil.oc.api.network._
import li.cil.oc.api.network.{Node => ImmutableNode}
import li.cil.oc.common.item.data.NodeData
import li.cil.oc.server.driver.CompoundBlockEnvironment
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.machine.ArgumentsImpl
import li.cil.oc.server.machine.Callbacks
import li.cil.oc.server.machine.Callbacks.ComponentCallback
import li.cil.oc.server.machine.Callbacks.PeripheralCallback
import li.cil.oc.server.machine.Machine
import li.cil.oc.util.SideTracker
import net.minecraft.nbt.NBTTagCompound

import scala.jdk.CollectionConverters.*

trait Component extends network.Component with li.cil.oc.server.network.Node {
  def visibility: Visibility = _visibility

  private lazy val callbacks = Callbacks(host)

  private lazy val hosts = host match {
    case multi: CompoundBlockEnvironment =>
      callbacks.map {
        case (method, callback) => callback match {
          case component: ComponentCallback =>
            multi.environments.find {
              case (_, environment) => environment.getClass == component.method.getDeclaringClass
            } match {
              case Some((_, environment)) => method -> Some(environment)
              case _ => method -> None
            }
          case peripheral: PeripheralCallback =>
            multi.environments.find {
              case (_, environment: ManagedPeripheral) => environment.methods.contains(peripheral.annotation.value)
              case _ => false
            } match {
              case Some((_, environment)) => method -> Some(environment)
              case _ => method -> None
            }
        }
      }
    case _ => callbacks.map {
      case (method, callback) => method -> Some(host)
    }
  }

  private var _visibility = Visibility.None

  def setVisibility(value: Visibility) = {
    if (value.ordinal() > reachability.ordinal()) {
      throw new IllegalArgumentException("Trying to set computer visibility to '" + value + "' on a '" + name +
        "' node with reachability '" + reachability + "'. It will be limited to the node's reachability.")
    }
    if (SideTracker.isServer) {
      if (network != null) _visibility match {
        case Visibility.Neighbors => value match {
          case Visibility.Network => addTo(reachableNodes.asScala)
          case Visibility.None => removeFrom(neighbors.asScala)
          case _ =>
        }
        case Visibility.Network => value match {
          case Visibility.Neighbors =>
            val neighborSet = neighbors.asScala.toSet
            removeFrom(reachableNodes.asScala.filterNot(neighborSet.contains))
          case Visibility.None => removeFrom(reachableNodes.asScala)
          case _ =>
        }
        case Visibility.None => value match {
          case Visibility.Neighbors => addTo(neighbors.asScala)
          case Visibility.Network => addTo(reachableNodes.asScala)
          case _ =>
        }
      }
      _visibility = value
    }
  }

  def canBeSeenFrom(other: ImmutableNode) = visibility match {
    case Visibility.None => false
    case Visibility.Network => canBeReachedFrom(other)
    case Visibility.Neighbors => isNeighborOf(other)
  }

  private def addTo(nodes: Iterable[ImmutableNode]) = nodes.foreach(_.host match {
    case machine: Machine => machine.addComponent(this)
    case _ =>
  })

  private def removeFrom(nodes: Iterable[ImmutableNode]) = nodes.foreach(_.host match {
    case machine: Machine => machine.removeComponent(this)
    case _ =>
  })

  // ----------------------------------------------------------------------- //

  override def methods = callbacks.keySet.asJava

  override def annotation(method: String) =
    callbacks.get(method) match {
      case Some(callback) => callbacks(method).annotation
      case _ => throw new NoSuchMethodException()
    }

  override def invoke(method: String, context: Context, arguments: AnyRef*): Array[AnyRef] = {
    callbacks.get(method) match {
      case Some(callback) => hosts(method) match {
        case Some(environment) => Registry.convert(callback(environment, context, new ArgumentsImpl(Seq(arguments*))))
        case _ => throw new NoSuchMethodException()
      }
      case _ => throw new NoSuchMethodException()
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound): Unit = {
    super.load(nbt)
    if (nbt.hasKey(NodeData.VisibilityTag)) {
      _visibility = Visibility.values()(nbt.getInteger(NodeData.VisibilityTag))
    }
  }

  override def save(nbt: NBTTagCompound): Unit = {
    super.save(nbt)
    nbt.setInteger(NodeData.VisibilityTag, _visibility.ordinal())
  }

  override def toString = super.toString + s"@$name"
}