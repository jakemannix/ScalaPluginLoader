package xyz.janboerman.scalaloader.example.scala

import scala.collection.JavaConverters._
import org.bukkit.{Material, World}
import org.bukkit._
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.command.{Command, CommandSender}
import org.bukkit.event.{EventPriority, Listener}
import org.bukkit.permissions.PermissionDefault
import xyz.janboerman.scalaloader.event.EventBus
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription.{Command => SPCommand, Permission => SPPermission}
import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}
import xyz.janboerman.scalaloader.plugin.description.{Api, ApiVersion, Scala, ScalaVersion}

@Scala(version = ScalaVersion.v2_12_11)
@Api(ApiVersion.v1_15)
object ExamplePlugin
    extends ScalaPlugin(new ScalaPluginDescription("ScalaExample", "0.13.4-SNAPSHOT")
        .addCommand(new SPCommand("foo") permission "scalaexample.foo")
        .addCommand(new SPCommand("home") permission "scalaexample.home" usage "/home set|tp")
        .permissions(new SPPermission("scalaexample.home") permissionDefault PermissionDefault.TRUE)) {

    getLogger.info("ScalaExample - I am constructed!")

    override def onLoad(): Unit = {
        getLogger.info("ScalaExample - I am loaded!")
    }

    override def onEnable(): Unit = {
        getLogger.info("ScalaExample - I am enabled!")
        val maybeNether = getServer.getWorlds.asScala.toList.find(_.getEnvironment == World.Environment.NETHER)
        maybeNether
          .map(generateNetherSpawnLocation)
          .getOrElse(getLogger.warning("The nether was not loaded onEnable of the plugin!"))
        PlayerJoinListener.nether = maybeNether.get
        PlayerJoinListener.logger = getLogger

        eventBus.registerEvents(PlayerJoinListener, this)
        eventBus.registerEvents(RandomHomeTeleportBlocker, this)
        eventBus.registerEvent(classOf[HomeTeleportEvent],
            new Listener() {}, EventPriority.MONITOR, (l: Listener, ev: HomeTeleportEvent) => {
            if (ev.isCancelled) {
                getLogger.info("Player " + ev.player.getName + " tried to teleport home, but couldn't!")
            } else {
                getLogger.info("Player " + ev.player.getName + " teleported home!")
            }
        }, this, false);
        getCommand("home").setExecutor(HomeExecutor)
        listConfigs()
        checkMaterials()
    }

    def generateNetherSpawnLocation(nether: World) = {
      getLogger.info("The nether was already available onEnable of the plugin")
      val chunk0 = nether.getChunkAt(0, 0)
      val maybeNetherSpawnLocation = findNetherSpawnLocation(nether, chunk0, 100)
      
    }


    private def findNetherSpawnLocation(nether: World, startingChunk: Chunk, countDown: Int): Option[Location] = {
        if (countDown <= 0) {
            None
        } else {
            getLogger.info(s"about to load chunk $countDown")
            load(startingChunk)
            val blocks: Array[Block] = (for {
                x <- 0 until 16
                z <- 0 until 16
                y <- 0 until 128
            } yield {
                startingChunk.getBlock(x, y, z)
            }).toArray
            //Collect block stats and log it *here*
            //currently not finding netherbrick
            val blockStats: Map[Material, Int] = blocks.map(_.getType).groupBy(identity).mapValues(_.length)
            getLogger.info(s"$blockStats")
            val maybeFirstNetherBrick: Option[Block] = blocks.find(_.getType == Material.NETHER_BRICK)
            maybeFirstNetherBrick
              .map(_.getLocation)
              .orElse(findNetherSpawnLocation(nether, getNextChunk(nether, startingChunk), countDown - 1))
        }
    }

    private def getNextChunk(nether: World, chunk: Chunk): Chunk = nether.getChunkAt(chunk.getX + 16, chunk.getZ)

    private def load(chunk: Chunk) = {
        if (!chunk.isLoaded) {
            val actuallyLoaded = chunk.load(true)
            if (!actuallyLoaded) {
                getLogger.severe(s"was unable to load chunk at: ${chunk.getX}, ${chunk.getZ}")
            }
        }
    }

    override def onDisable(): Unit = {
        getLogger.info("ScalaExample - I am disabled!")
    }

    override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
        sender.sendMessage("Executed foo command!")
        true
    }

    def getInt() = 42

    private def listConfigs(): Unit = {
        val urls = getClassLoader.findResources("config.yml")
        while (urls.hasMoreElements) {
            val url = urls.nextElement()
            getLogger.info(s"Found resource: ${url.toString}")
        }
    }

    private def checkMaterials(): Unit = {
        val (legacy, modern) = Material.values().partition(_.isLegacy)
        val foundModern = modern.isEmpty
        val foundLegacy = legacy.isEmpty

        getLogger.info(s"Found legacy material?: $foundLegacy, modern material?: $foundModern")
        if (foundModern == foundLegacy)
            getLogger.info("This is a pluginloader bug!")
        else
            getLogger.info("Materials work as intended!")
    }

    def eventBus: EventBus = super.getEventBus
}

