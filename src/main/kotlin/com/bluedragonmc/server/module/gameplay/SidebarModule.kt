package com.bluedragonmc.server.module.gameplay

import com.bluedragonmc.server.ALT_COLOR_1
import com.bluedragonmc.server.BRAND_COLOR_PRIMARY_1
import com.bluedragonmc.server.Game
import com.bluedragonmc.server.SERVER_IP
import com.bluedragonmc.server.module.GameModule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.scoreboard.Sidebar

/**
 * A module that shows a sidebar to all players in the game.
 */
class SidebarModule(title: String) : GameModule() {
    private lateinit var parent: Game
    private val sidebar: Sidebar = Sidebar(Component.text(title.uppercase(), ALT_COLOR_1, TextDecoration.BOLD))
    override fun initialize(parent: Game, eventNode: EventNode<Event>) {
        this.parent = parent
        sidebar.createLine(Sidebar.ScoreboardLine("website", Component.text(SERVER_IP, BRAND_COLOR_PRIMARY_1), 0))
        parent.players.forEach { sidebar.addViewer(it) }
        eventNode.addListener(PlayerSpawnEvent::class.java) { event ->
            sidebar.addViewer(event.player)
        }
        eventNode.addListener(RemoveEntityFromInstanceEvent::class.java) { event ->
            if (event.entity !is Player) return@addListener
            sidebar.removeViewer(event.entity as Player)
        }
    }

    /**
     * Adds a new line above all existing lines.
     */
    fun addLine(id: String, line: Component) {
        sidebar.createLine(Sidebar.ScoreboardLine(id, line, sidebar.lines.size))
    }

    /**
     * Update an existing line based on its ID.
     */
    fun updateLine(id: String, newLine: Component) {
        sidebar.updateLineContent(id, newLine)
    }
}