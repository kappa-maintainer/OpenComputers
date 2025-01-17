package li.cil.oc.client

import java.net.MalformedURLException
import java.net.URL
import java.net.URI
import java.net.URLConnection
import java.net.URLStreamHandler
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import com.google.common.base.Charsets
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.SoundManager
import net.minecraft.server.integrated.IntegratedServer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundCategory
import net.minecraftforge.client.event.sound.SoundLoadEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.client.FMLClientHandler
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import paulscode.sound.SoundSystemConfig

import java.io.InputStream
import scala.collection.mutable
import scala.io.Source

object Sound {
  private val sources = mutable.Map.empty[TileEntity, PseudoLoopingStream]

  private val commandQueue = mutable.PriorityQueue.empty[Command]

  private var lastVolume = FMLClientHandler.instance.getClient.gameSettings.getSoundLevel(SoundCategory.BLOCKS)

  private val updateTimer = new Timer("OpenComputers-SoundUpdater", true)
  if (Settings.get.soundVolume > 0)
    updateTimer.scheduleAtFixedRate(new TimerTask {
      override def run(): Unit = {
        sources.synchronized {
          updateCallable = Some(() => {
            updateVolume()
            processQueue()
          })
        }
      }
    }, 500, 50)

  private var updateCallable = None: Option[() => Unit]

  // Set in init event.
  var manager: SoundManager = scala.compiletime.uninitialized

  def soundSystem: SoundManager#SoundSystemStarterThread = if (manager != null) manager.sndSystem else null

  private def updateVolume(): Unit = {
    val volume =
      if (isGamePaused) 0f
      else FMLClientHandler.instance.getClient.gameSettings.getSoundLevel(SoundCategory.BLOCKS)
    if (volume != lastVolume) {
      lastVolume = volume
      sources.synchronized {
        for (sound <- sources.values) {
          sound.updateVolume()
        }
      }
    }
  }

  private def isGamePaused = {
    val server = FMLCommonHandler.instance.getMinecraftServerInstance
    // Check outside of match to avoid client side class access.
    server != null && !server.isDedicatedServer && (server match {
      case integrated: IntegratedServer => Minecraft.getMinecraft.isGamePaused
      case _ => false
    })
  }

  private def processQueue(): Unit = {
    if (commandQueue.nonEmpty) {
      commandQueue.synchronized {
        while (commandQueue.nonEmpty && commandQueue.head.when < System.currentTimeMillis()) {
          try
            commandQueue.dequeue()()
          catch
            case t: Throwable => OpenComputers.log.warn("Error processing sound command.", t)

        }
      }
    }
  }

  def startLoop(tileEntity: TileEntity, name: String, volume: Float = 1f, delay: Long = 0): Unit = {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new StartCommand(System.currentTimeMillis() + delay, tileEntity, name, volume)
      }
    }
  }

  def stopLoop(tileEntity: TileEntity): Unit = {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new StopCommand(tileEntity)
      }
    }
  }

  def updatePosition(tileEntity: TileEntity): Unit = {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new UpdatePositionCommand(tileEntity)
      }
    }
  }

  @SubscribeEvent
  def onSoundLoad(event: SoundLoadEvent): Unit = {
    manager = event.getManager
  }

  private var hasPreloaded = Settings.get.soundVolume <= 0

  @SubscribeEvent
  def onTick(e: ClientTickEvent): Unit =
    if (soundSystem != null)
      if (!hasPreloaded)
        hasPreloaded = true
        new Thread(new Runnable() {
          override def run(): Unit = {
            val preloadConfigLocation = new ResourceLocation(Settings.resourceDomain, "sounds/preload.cfg")
            val preloadConfigResource = Minecraft.getMinecraft.getResourceManager.getResource(preloadConfigLocation)
            for (location <- Source.fromInputStream(preloadConfigResource.getInputStream)(Charsets.UTF_8).getLines()) {
              val url = getClass.getClassLoader.getResource(location)
              if (url != null)
                try {
                  val sourceName = "preload_" + location
                  soundSystem.newSource(false, sourceName, url, location, true, 0, 0, 0, SoundSystemConfig.ATTENUATION_NONE, 16)
                  soundSystem.activate(sourceName)
                  soundSystem.removeSource(sourceName)
                }
                catch {
                  case _: Throwable => // Meh.
                }
              else OpenComputers.log.warn(s"Couldn't preload sound $location!")
            }
          }
        })

      sources.synchronized {
        updateCallable.foreach(_())
        updateCallable = None
      }

  @SubscribeEvent
  def onWorldUnload(event: WorldEvent.Unload): Unit = {
    commandQueue.synchronized(commandQueue.clear())
    sources.synchronized(
      try {
        sources.foreach(_._2.stop())
      } catch {
        case _: Throwable => // Ignore.
      }
    )
    sources.clear()
  }

  private abstract class Command(val when: Long, val tileEntity: TileEntity) extends Ordered[Command] {
    def apply(): Unit

    override def compare(that: Command) = (that.when - when).toInt
  }

  private class StartCommand(when: Long, tileEntity: TileEntity, val name: String, val volume: Float) extends Command(when, tileEntity) {
    override def apply(): Unit =
      sources.synchronized {
        sources.getOrElseUpdate(tileEntity, new PseudoLoopingStream(tileEntity, volume)).play(name)
      }

  }

  private class StopCommand(tileEntity: TileEntity) extends Command(System.currentTimeMillis() + 1, tileEntity) {
    override def apply(): Unit = {
      sources.synchronized {
        sources.remove(tileEntity) match {
          case Some(sound) => sound.stop()
          case _ =>
        }
      }
      commandQueue.synchronized {
        // Remove all other commands for this tile entity from the queue. This
        // is inefficient, but we generally don't expect the command queue to
        // be very long, so this should be OK.
        commandQueue ++= commandQueue.dequeueAll.filter(_.tileEntity != tileEntity)
      }
    }
  }

  private class UpdatePositionCommand(tileEntity: TileEntity) extends Command(System.currentTimeMillis(), tileEntity) {
    override def apply(): Unit = {
      sources.synchronized {
        sources.get(tileEntity) match {
          case Some(sound) => sound.updatePosition()
          case _ =>
        }
      }
    }
  }

  private class PseudoLoopingStream(val tileEntity: TileEntity, val volume: Float, val source: String = UUID.randomUUID.toString) {
    var initialized = false

    def updateVolume(): Unit = soundSystem.setVolume(source, lastVolume * volume * Settings.get.soundVolume)

    def updatePosition(): Unit =
      if (tileEntity != null) soundSystem.setPosition(source, tileEntity.getPos.getX.toFloat, tileEntity.getPos.getY.toFloat, tileEntity.getPos.getZ.toFloat)
      else soundSystem.setPosition(source, 0, 0, 0)

    def play(name: String): Unit = {
      val resourceName = s"${Settings.resourceDomain}:$name"
      val sound = manager.sndHandler.getAccessor(new ResourceLocation(resourceName))
      // Specified return type because apparently this is ambiguous according to Jenkins. I don't even.
      val resource = (sound.cloneEntry(): net.minecraft.client.audio.Sound).getSoundAsOggLocation
      if (!initialized) {
        initialized = true
        if (tileEntity != null) soundSystem.newSource(false, source, toUrl(resource), resource.toString, true, tileEntity.getPos.getX.toFloat, tileEntity.getPos.getY.toFloat, tileEntity.getPos.getZ.toFloat, SoundSystemConfig.ATTENUATION_LINEAR, 16)
        else soundSystem.newSource(false, source, toUrl(resource), resource.toString, false, 0, 0, 0, SoundSystemConfig.ATTENUATION_NONE, 0)
        updateVolume()
        soundSystem.activate(source)
      }
      soundSystem.play(source)
    }

    def stop(): Unit = {
      if (soundSystem != null) try {
        soundSystem.stop(source)
        soundSystem.removeSource(source)
      }
      catch {
        case _: Throwable =>
      }
    }
  }

  // This is copied from SoundManager.getURLForSoundResource, which is private.
  private def toUrl(resource: ResourceLocation): URL = {
    val name = s"mcsounddomain:${resource.getNamespace}:${resource.getPath}"
    try
      URL.of(URI.create(name), (url: URL) => new URLConnection(url) {
        def connect(): Unit = {}

        override def getInputStream: InputStream =
          try
            Minecraft.getMinecraft.getResourceManager.getResource(resource).getInputStream
          catch
            case t: Throwable => {
              OpenComputers.log.warn(t)
              throw t
            }
      })
    catch
      case _: MalformedURLException => null
  }
}
