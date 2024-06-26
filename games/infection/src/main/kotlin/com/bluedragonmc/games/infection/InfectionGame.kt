package com.bluedragonmc.games.infection

import com.bluedragonmc.games.infection.module.InfectionModule
import com.bluedragonmc.server.Game
import com.bluedragonmc.server.module.GuiModule
import com.bluedragonmc.server.module.combat.CustomDeathMessageModule
import com.bluedragonmc.server.module.combat.OldCombatModule
import com.bluedragonmc.server.module.database.AwardsModule
import com.bluedragonmc.server.module.database.StatisticsModule
import com.bluedragonmc.server.module.gameplay.InventoryPermissionsModule
import com.bluedragonmc.server.module.gameplay.MaxHealthModule
import com.bluedragonmc.server.module.gameplay.SidebarModule
import com.bluedragonmc.server.module.gameplay.WorldPermissionsModule
import com.bluedragonmc.server.module.instance.SharedInstanceModule
import com.bluedragonmc.server.module.map.AnvilFileMapProviderModule
import com.bluedragonmc.server.module.minigame.*
import com.bluedragonmc.server.module.vanilla.NaturalRegenerationModule
import com.bluedragonmc.server.utils.noItalic
import com.bluedragonmc.server.utils.withColor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.minestom.server.entity.GameMode
import net.minestom.server.item.Enchantment
import net.minestom.server.item.ItemMeta
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import java.nio.file.Paths

class InfectionGame(mapName: String) : Game("Infection", mapName) {
    private val stickItem = ItemStack.builder(Material.STICK).displayName(Component.text("Knockback Stick"))
        .lore(
            Component.translatable("game.infection.kb_stick.lore.1", NamedTextColor.GRAY).noItalic(),
            Component.translatable("game.infection.kb_stick.lore.2", NamedTextColor.GRAY).noItalic()
        )
        .meta { metaBuilder: ItemMeta.Builder ->
            metaBuilder.enchantment(Enchantment.KNOCKBACK, 3)
        }.build()

    init {
        // MAP
        use(AnvilFileMapProviderModule(Paths.get("worlds/$name/$mapName")))

        // INSTANCE
        use(SharedInstanceModule())

        // COMBAT
        use(OldCombatModule(allowDamage = false, allowKnockback = true))
        use(CustomDeathMessageModule())

        // GAMEPLAY
        use(AwardsModule())
        use(InventoryPermissionsModule(allowDropItem = false, allowMoveItem = false))
        use(GuiModule())
        use(
            KitsModule(
                showMenu = false, giveKitsOnStart = true, selectableKits = listOf(
                    KitsModule.Kit(
                        Component.translatable("game.infection.kit.default"),
                        Component.translatable("game.infection.kit.default.desc"),
                        Material.STICK,
                        hashMapOf(0 to stickItem)
                    )
                )
            )
        )
        use(MaxHealthModule(1.0F))
        use(MOTDModule(Component.translatable("game.infection.motd")))
        use(NaturalRegenerationModule())
        use(PlayerResetModule(defaultGameMode = GameMode.ADVENTURE))
        val sidebarModule = SidebarModule(name)
        val bind = sidebarModule.bind {
            players.map {
                val color = if (getModuleOrNull<InfectionModule>()?.isInfected(it) == true) RED else GREEN
                val component = (it.name withColor color)
                SidebarModule.SidebarLine(it.uuid.toString(), component)
            }
        }
        use(SpawnpointModule(SpawnpointModule.DatabaseSpawnpointProvider(allowRandomOrder = true)))
        use(TeamModule(autoTeams = false))
        use(SpectatorModule(spectateOnDeath = false, spectateOnLeave = true))
        use(TimedRespawnModule(5))
        use(WorldPermissionsModule(allowBlockInteract = true))

        // MINIGAME
        use(CountdownModule(threshold = 2, allowMoveDuringCountdown = true))
        use(WinModule(winCondition = WinModule.WinCondition.MANUAL) { player, winningTeam ->
            if (player in winningTeam.players) 100 else 10
        })

        use(InfectionModule(scoreboardBinding = bind))

        use(StatisticsModule())

        ready()
    }
}