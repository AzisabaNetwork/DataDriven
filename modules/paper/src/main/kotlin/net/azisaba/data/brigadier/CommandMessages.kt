package net.azisaba.data.brigadier

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.jetbrains.annotations.ApiStatus

/**
 * Creates an informational command message with the DataDriven prefix.
 */
@ApiStatus.Internal
internal fun withInfoPrefix(body: TextComponent.Builder.() -> Unit): Component {
    return buildPrefixed(
        Component.text().color(NamedTextColor.GRAY).apply(body).build(),
        TextColor.color(52, 159, 218),
    )
}

/**
 * Creates a successful command message with the DataDriven prefix.
 */
@ApiStatus.Internal
internal fun withSuccessPrefix(body: TextComponent.Builder.() -> Unit): Component {
    return buildPrefixed(
        Component.text().color(NamedTextColor.GRAY).apply(body).build(),
        TextColor.color(0, 176, 107),
    )
}

/**
 * Creates a failure command message with the DataDriven prefix.
 */
@ApiStatus.Internal
internal fun withFailurePrefix(body: TextComponent.Builder.() -> Unit): Component {
    return buildPrefixed(
        Component.text().color(NamedTextColor.GRAY).apply(body).build(),
        TextColor.color(255, 75, 0),
    )
}

private fun buildPrefixed(body: Component, color: TextColor): Component {
    return Component.text()
        .append(buildPrefix(color))
        .appendSpace()
        .append(body)
        .build()
}

private fun buildPrefix(color: TextColor): Component {
    return Component.text()
        .append(Component.text("[", NamedTextColor.GRAY))
        .append(Component.text("Data", color))
        .append(Component.text("] ", NamedTextColor.GRAY))
        .hoverEvent(
            HoverEvent.showText(
                Component.text()
                    .append(Component.text("ℹ Powered by DataDriven API"))
                    .appendNewline()
                    .append(
                        Component.text(
                            "https://github.com/AzisabaNetwork/DataDriven",
                            NamedTextColor.BLUE,
                            TextDecoration.UNDERLINED,
                        )
                    )
                    .build()
            )
        )
        .build()
}
