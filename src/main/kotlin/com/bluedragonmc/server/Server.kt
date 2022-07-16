package com.bluedragonmc.server

import com.bluedragonmc.server.Environment.messagingDisabled
import com.bluedragonmc.server.Environment.queue
import com.bluedragonmc.server.command.*
import com.bluedragonmc.server.game.Lobby
import com.bluedragonmc.server.module.gameplay.SpawnpointModule
import com.bluedragonmc.server.utils.buildComponent
import com.bluedragonmc.server.utils.plus
import com.bluedragonmc.server.utils.withColor
import com.bluedragonmc.server.utils.withDecoration
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.server.ServerListPingEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.extras.lan.OpenToLAN
import net.minestom.server.ping.ServerListPingType
import org.slf4j.LoggerFactory
import java.net.InetAddress

lateinit var lobby: Game
val queue = Environment.queue
private val logger = LoggerFactory.getLogger("ServerKt")

fun main() {
    logger.info("Using queue type: ${queue::class.simpleName}")
    val minecraftServer = MinecraftServer.init()

    // Create a test instance
    lobby = Lobby()

    // Make players spawn in the test instance
    MinecraftServer.getGlobalEventHandler().addListener(PlayerLoginEvent::class.java) { event ->
        event.player.respawnPoint = lobby.getModule<SpawnpointModule>().spawnpointProvider.getSpawnpoint(event.player)
        event.setSpawningInstance(lobby.getInstance())
    }

    // Chat formatting
    MinecraftServer.getGlobalEventHandler().addListener(PlayerChatEvent::class.java) { event ->
        val experience = (event.player as CustomPlayer).data.experience
        val level = CustomPlayer.getXpLevel(experience)
        val xpToNextLevel = CustomPlayer.getXpToNextLevel(level, experience).toInt()
        event.setChatFormat {
            Component.join(
                JoinConfiguration.noSeparators(),
                Component.text("[", NamedTextColor.DARK_GRAY),
                Component.text(level.toInt(), BRAND_COLOR_PRIMARY_1).hoverEvent(
                    HoverEvent.showText(event.player.name + Component.text(" has a total of $experience experience,\nand needs $xpToNextLevel XP to reach level ${level.toInt()+1}."))),
                Component.text("] ", NamedTextColor.DARK_GRAY),
                event.player.name,
                Component.text(": ", NamedTextColor.DARK_GRAY),
                Component.text(event.message, NamedTextColor.WHITE)
            )
        }
    }

    MinecraftServer.getGlobalEventHandler().addListener(ServerListPingEvent::class.java) { event ->
        event.responseData.description = buildComponent {
            +("Blue" withColor BRAND_COLOR_PRIMARY_2 withDecoration TextDecoration.BOLD)
            +("Dragon" withColor BRAND_COLOR_PRIMARY_1 withDecoration TextDecoration.BOLD)
            +(" [" withColor NamedTextColor.DARK_GRAY)
            if (messagingDisabled) {
                +("Dev on ${InetAddress.getLocalHost().hostName}" withColor NamedTextColor.RED)
            } else {
                +(event.responseData.version withColor NamedTextColor.GREEN)
            }
            +("]" withColor NamedTextColor.DARK_GRAY)
            if(event.pingType != ServerListPingType.OPEN_TO_LAN) { // Newlines are disallowed in Open To LAN pings
                +Component.newline()
                +SERVER_NEWS
            }
        }
        event.responseData.favicon = FAVICON
    }

    // Initialize commands
    listOf(
        JoinCommand("join", "/join <game>"),
        InstanceCommand("instance", "/instance <list|add|remove> ...", "in"),
        GameCommand("game", "/game <start|end>"),
        LobbyCommand("lobby", "/lobby", "l", "hub"),
        TeleportCommand("tp", "/tp <player> | /tp <x> <y> <z>"),
        FlyCommand("fly"),
        GameModeCommand("gamemode", "/gamemode <survival|creative|adventure|spectator> [player]", "gm"),
        KillCommand("kill", "/kill [player]"),
        SetBlockCommand("setblock", "/setblock <x> <y> <z> <block>"),
        PartyCommand("party", "/party <invite|kick|promote|warp|chat|list> ...", "p"),
        GiveCommand("give", "/give [player] <item>")
    ).forEach(MinecraftServer.getCommandManager()::register)

    // Set a custom player provider, so we can easily add fields to the Player class
    MinecraftServer.getConnectionManager().setPlayerProvider(::CustomPlayer)

    // Start the queue loop, which runs every 2 seconds and handles the players in queue
    queue.start()

    // Enable Mojang authentication (if we add a proxy, disable this)
    MojangAuth.init()

    // Start the server & bind to port 25565
    minecraftServer.start("0.0.0.0", 25565)

    if(Environment.isDev()) OpenToLAN.open()
}
