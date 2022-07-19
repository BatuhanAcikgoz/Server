package com.bluedragonmc.server

import com.bluedragonmc.server.utils.noBold
import com.bluedragonmc.server.utils.plus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.apache.commons.net.util.Base64
import java.io.File
import java.nio.charset.Charset

/**
 * Light color, often used for emphasis.
 */
val BRAND_COLOR_PRIMARY_1 = TextColor.color(0x4EB2F4)

/**
 * Medium color, often used for chat messages.
 */
val BRAND_COLOR_PRIMARY_2 = TextColor.color(0x2792f7) // Medium, often used for chat messages

/**
 * Very dark color.
 */
val BRAND_COLOR_PRIMARY_3 = TextColor.color(0x3336f4) // Very dark

/**
 * An alternate color, used for the title in the scoreboard and some chat messages.
 */
val ALT_COLOR_1 = NamedTextColor.YELLOW

/**
 * A secondary alternate color that complements [ALT_COLOR_1].
 */
val ALT_COLOR_2 = NamedTextColor.GOLD

/**
 * The hostname of the server, used in the scoreboard footer.
 */
const val SERVER_IP = "bluedragonmc.com"

/**
 * Information about the latest update, displayed in the server list description.
 */
val SERVER_NEWS = Component.text("              NEW GAME", NamedTextColor.GOLD, TextDecoration.BOLD) +
        Component.text(" - ", NamedTextColor.GRAY).noBold() +
        Component.text("SKYWARS", NamedTextColor.YELLOW, TextDecoration.BOLD)

/**
 * A base64-encoded PNG image of the server's favicon shown on clients' server lists.
 */
val FAVICON = "data:image/png;base64," + String(Base64.encodeBase64(File("favicon_64.png").readBytes()), Charset.forName("UTF-8"))