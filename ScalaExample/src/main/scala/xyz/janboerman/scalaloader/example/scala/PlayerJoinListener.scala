package xyz.janboerman.scalaloader.example.scala

import java.util.logging.Logger

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit._

object PlayerJoinListener extends Listener {
  var nether: World = _
  var maybeFortressLocation: Option[Location] = None
  var logger: Logger = _

  @EventHandler
  def onJoin(event: PlayerJoinEvent): Unit = {
    val player = event.getPlayer
    event.setJoinMessage(ChatColor.GREEN + "Howdy " + player.getName + "!")
    setupFortressLocation(player)
    maybeFortressLocation.foreach(player.teleport)
  }

  def setupFortressLocation(player: Player) = {
    if (maybeFortressLocation.isEmpty) {
      val (x, z) = (player.getLocation.getBlockX, player.getLocation.getBlockZ)
      val netherChunk: Chunk = nether.getChunkAt(x, z)
      val netherBlock = netherChunk.getBlock(0, 0, 0)
//      nether.createExplosion(netherBlock.getLocation, 4F, false)
//      maybeFortressLocation = Some(netherBlock.getLocation)
      maybeFortressLocation = netherFortressPosition(netherChunk, 1000)
      if (maybeFortressLocation.isEmpty) {
        logger.warning("Tried to find a fortress, but failed, sadness!")
      }
    }
  }

  private def netherFortressPosition(startingChunk: Chunk, countDown: Int): Option[Location] = {
    if (countDown <= 0) {
      None
    } else {
      logger.info(s"about to load chunk $countDown")
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
      logger.info(s"$blockStats")
      val maybeFirstNetherBrick: Option[Block] = blocks.find(_.getType == Material.NETHER_BRICK)
      maybeFirstNetherBrick
        .map(_.getLocation)
        .orElse(netherFortressPosition(getNextChunk(startingChunk), countDown - 1))
    }
  }

  private def getNextChunk(chunk: Chunk): Chunk = nether.getChunkAt(chunk.getX + 16, chunk.getZ)

  private def load(chunk: Chunk) = {
    if (!chunk.isLoaded) {
      val actuallyLoaded = chunk.load(true)
      if (!actuallyLoaded) {
        logger.severe(s"was unable to load chunk at: ${chunk.getX}, ${chunk.getZ}")
      }
    }
  }
}
