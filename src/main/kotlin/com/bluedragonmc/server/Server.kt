package com.bluedragonmc.server

import com.bluedragonmc.messages.ReportErrorMessage
import com.bluedragonmc.messages.SendPlayerToInstanceMessage
import com.bluedragonmc.server.Environment.queue
import com.bluedragonmc.server.block.SignHandler
import com.bluedragonmc.server.block.SkullHandler
import com.bluedragonmc.server.command.*
import com.bluedragonmc.server.command.punishment.*
import com.bluedragonmc.server.game.Lobby
import com.bluedragonmc.server.module.database.DatabaseModule
import com.bluedragonmc.server.module.database.PermissionGroup
import com.bluedragonmc.server.module.database.Permissions
import com.bluedragonmc.server.module.database.Punishment
import com.bluedragonmc.server.module.messaging.MessagingModule
import com.bluedragonmc.server.utils.*
import com.bluedragonmc.server.utils.packet.PerInstanceTabList
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.server.ServerListPingEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.extras.lan.OpenToLAN
import net.minestom.server.extras.velocity.VelocityProxy
import net.minestom.server.instance.Instance
import net.minestom.server.ping.ServerListPingType
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.time.Duration
import java.util.*
import kotlin.system.exitProcess

lateinit var lobby: Game
val queue = Environment.queue
private val logger = LoggerFactory.getLogger("ServerKt")

fun main() {
    logger.info("Using queue type: ${queue::class.simpleName}")
    val minecraftServer = MinecraftServer.init()
    val eventNode = MinecraftServer.getGlobalEventHandler()

    // Create a test instance
    lobby = Lobby()

    // Send players to the instance they are supposed to join instead of the lobby,
    // if a SendPlayerToInstanceMessage was received before they joined.
    val futureInstances = mutableMapOf<UUID, Instance>()

    MessagingModule.subscribe(SendPlayerToInstanceMessage::class) { message ->
        val instance = MinecraftServer.getInstanceManager().getInstance(message.instance)
        if (instance != null && MinecraftServer.getConnectionManager().getPlayer(message.player) == null) {
            futureInstances[message.player] = instance
        }
    }

    // Make players spawn in the correct instance
    MinecraftServer.getGlobalEventHandler().addListener(PlayerLoginEvent::class.java) { event ->
        val instance = futureInstances[event.player.uuid] ?: lobby.getInstance()
        val game = Game.findGame(instance.uniqueId)
        if (event.player.displayName == null)
            event.player.displayName = Component.text(
                event.player.username,
                BRAND_COLOR_PRIMARY_1 // The default color that appears before group data is loaded
            )
        event.player.sendMessage(Component.text("Placing you in ${instance.uniqueId}...", NamedTextColor.DARK_GRAY))
        event.setSpawningInstance(instance)
        game?.players?.add(event.player)
        futureInstances.remove(event.player.uuid)

        event.player.sendPlayerListHeaderAndFooter(
            SERVER_NAME_GRADIENT.decorate(TextDecoration.BOLD),
            (" Join our community at " withColor BRAND_COLOR_PRIMARY_2) + ("bluedragonmc.com " withColor BRAND_COLOR_PRIMARY_1))
    }

    // Chat formatting
    eventNode.addListener(PlayerChatEvent::class.java) { event ->
        val player = event.player as CustomPlayer
        player.getFirstMute()?.let { mute ->
            event.isCancelled = true
            event.player.sendMessage(getPunishmentMessage(mute, "currently muted").surroundWithSeparators())
            return@addListener
        }
        val experience = (player).run { if (isDataInitialized()) data.experience else 0 }
        val level = CustomPlayer.getXpLevel(experience)
        val xpToNextLevel = CustomPlayer.getXpToNextLevel(level, experience)

        val group = player.data.highestGroup ?: PermissionGroup("default", NamedTextColor.GRAY)

        event.setChatFormat {
            Component.join(
                JoinConfiguration.noSeparators(),
                Component.text("[", NamedTextColor.DARK_GRAY),
                Component.text(level.toInt(), BRAND_COLOR_PRIMARY_1)
                    .hoverEvent(HoverEvent.showText(
                        player.name +
                                Component.text(" has a total of $experience experience,\nand needs $xpToNextLevel XP to reach level ${level.toInt() + 1}.",
                                    NamedTextColor.GRAY)
                    )),
                Component.text("] ", NamedTextColor.DARK_GRAY),
                group.prefix,
                if (group.prefix != Component.empty()) Component.space() else Component.empty(),
                player.name,
                Component.text(": ", NamedTextColor.DARK_GRAY),
                if (!Permissions.hasPermission(player.data, "chat.minimessage"))
                    Component.text(event.message, NamedTextColor.WHITE)
                else MiniMessage.miniMessage().deserialize(event.message)
            )
        }
    }

    eventNode.addListener(ServerListPingEvent::class.java) { event ->
        event.responseData.description = (buildComponent {
            val title = Component.text("BlueDragon").withDecoration(TextDecoration.BOLD)
            if (event.pingType == ServerListPingType.OPEN_TO_LAN || (event.connection?.protocolVersion ?: 0) < 713)
                +title.withColor(BRAND_COLOR_PRIMARY_3)
            else
                +title.withGradient(BRAND_COLOR_PRIMARY_1, BRAND_COLOR_PRIMARY_3)

            +(" [" withColor NamedTextColor.DARK_GRAY)
            if (Environment.isDev()) {
                +("Dev on ${InetAddress.getLocalHost().hostName}" withColor NamedTextColor.RED)
            } else {
                +(event.responseData.version withColor NamedTextColor.GREEN)
            }
            +("]" withColor NamedTextColor.DARK_GRAY)
        }).center(92) + Component.newline() + buildComponent {
            if (event.connection != null && event.connection!!.protocolVersion < MinecraftServer.PROTOCOL_VERSION) {
                +("Update to Minecraft 1.18.2 to join BlueDragon." withColor NamedTextColor.RED)
                return@buildComponent
            }
            if (event.pingType != ServerListPingType.OPEN_TO_LAN) { // Newlines are disallowed in Open To LAN pings
                +SERVER_NEWS
            }
        }.center(92) // 115
            //Component.text("This string is going to be about 52 characters long, just as a test."))
        event.responseData.favicon = FAVICON
    }

    eventNode.addListener(DatabaseModule.DataLoadedEvent::class.java) { event ->
        val player = event.player as CustomPlayer
        val ban = player.getFirstBan()
        if (ban != null) {
            player.kick(getPunishmentMessage(ban, "currently banned from this server"))
        }

        val group = player.data.highestGroup
        if (group == null) {
            player.displayName = player.username withColor NamedTextColor.GRAY
            return@addListener
        }
        player.displayName = player.username withColor group.color
    }

    // Initialize commands
    listOf(
        JoinCommand("join", "/join <game>"),
        InstanceCommand("instance", "/instance <list|add|remove> ...", "in"),
        GameCommand("game", "/game <start|end>"),
        LobbyCommand("lobby", "/lobby", "l", "hub"),
        TeleportCommand("tp", "/tp <player|<x> <y> <z>> [player|<x> <y> <z>]"),
        FlyCommand("fly"),
        GameModeCommand("gamemode", "/gamemode <survival|creative|adventure|spectator> [player]", "gm", "gmc", "gms", "gma", "gmsp"),
        KillCommand("kill", "/kill [player]"),
        SetBlockCommand("setblock", "/setblock <x> <y> <z> <block>"),
        PartyCommand("party", "/party <invite|kick|promote|warp|chat|list> ...", "p"),
        GiveCommand("give", "/give [player] <item>"),
        PunishCommand("ban", "/<ban|mute> <player> <duration> <reason>", "mute"),
        KickCommand("kick", "/kick <player> <reason>"),
        PardonCommand("pardon", "/pardon <player|ban ID>", "unban", "unmute"),
        ViewPunishmentsCommand("punishments", "/punishments <player>", "vps", "history"),
        ViewPunishmentCommand("punishment", "/punishment <id>", "vp"),
        UpdateCommand("update", "/update <repo> <branch>"),
        PermissionCommand("permission", "/permission ...", "lp", "perm"),
        PingCommand("ping", "/ping", "latency"),
        MindecraftesCommand("mindecraftes", "/mindecraftes"),
        StopCommand("stop", "/stop")
    ).forEach(MinecraftServer.getCommandManager()::register)

    MinecraftServer.getCommandManager().setUnknownCommandCallback { sender, command ->
        sender.sendMessage(Component.translatable("commands.help.failed", NamedTextColor.RED))
    }

    // Create a per-instance tablist using custom packets
    PerInstanceTabList.hook(MinecraftServer.getGlobalEventHandler())

    // Set a custom player provider, so we can easily add fields to the Player class
    MinecraftServer.getConnectionManager().setPlayerProvider(::CustomPlayer)

    // Automatically report errors to Puffin in addition to logging them
    MinecraftServer.getExceptionManager().setExceptionHandler { e ->
        e.printStackTrace()
        DatabaseModule.IO.launch {
            MessagingModule.publish(
                ReportErrorMessage(
                    MessagingModule.containerId,
                    null,
                    e.message.orEmpty(),
                    e.stackTraceToString(),
                    getDebugContext()
                )
            )
        }
    }

    // Register custom dimension types
    MinecraftServer.getDimensionTypeManager().addDimension(
        DimensionType.builder(NamespaceID.from("bluedragon:fullbright_dimension")).ambientLight(1.0F).build()
    )

    MinecraftServer.getBlockManager().registerHandler("minecraft:sign", ::SignHandler)
    MinecraftServer.getBlockManager().registerHandler("minecraft:skull", ::SkullHandler)

    // Start the queue loop, which runs every 2 seconds and handles the players in queue
    queue.start()

    // Enable Velocity modern forwarding OR enable Mojang authentication
    System.getenv("PUFFIN_VELOCITY_SECRET")?.let { VelocityProxy.enable(it) } ?: MojangAuth.init()

    // Start the server & bind to port 25565
    minecraftServer.start("0.0.0.0", 25565)

    if (Environment.isDev()) OpenToLAN.open()

    if (!Environment.isDev()) { // Puffin/Docker environment

        // Make the server shutdown after 6 hours (if there are no players online)
        MinecraftServer.getSchedulerManager().buildTask {
            if (MinecraftServer.getConnectionManager().onlinePlayers.isEmpty()) {
                MinecraftServer.stopCleanly()
                exitProcess(0)
            }
        }.delay(Duration.ofHours(6)).repeat(Duration.ofSeconds(60)).schedule()
    }
}

fun getPunishmentMessage(punishment: Punishment, state: String) = buildComponent {
    +("You are $state!" withColor NamedTextColor.RED withDecoration TextDecoration.UNDERLINED)
    +Component.newline()
    +Component.newline()
    +("Reason: " withColor NamedTextColor.RED)
    +(punishment.reason withColor NamedTextColor.WHITE)
    +Component.newline()
    +("Expires in " withColor NamedTextColor.RED)
    +(punishment.getTimeRemaining() withColor NamedTextColor.WHITE)
    +Component.newline()
    +("Punishment ID: " withColor NamedTextColor.RED)
    +(punishment.id.toString().substringBefore("-") withColor NamedTextColor.WHITE)
}

fun getDebugContext() = mapOf(
    "Container ID" to MessagingModule.containerId.toString(),
    "All Running Instances" to MinecraftServer.getInstanceManager().instances.joinToString { it.uniqueId.toString() },
    "Running Games" to Game.games.joinToString { it.toString() },
    "Online Players" to MinecraftServer.getConnectionManager().onlinePlayers.joinToString { it.username }
)