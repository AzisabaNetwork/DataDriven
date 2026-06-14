package net.azisaba.data.brigadier

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.azisaba.data.config.ConfigHolder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import java.nio.file.Path
import kotlin.time.measureTime

/**
 * Creates a command node that reloads this configuration holder.
 *
 * @param T the configuration value type
 * @param literal the command literal
 * @param filePath the configuration file to reload
 * @param defaultValue the value written if [filePath] does not exist
 * @return the reload command node
 */
fun <T : Any> ConfigHolder<T>.asReloadCommandNode(
    literal: String,
    filePath: Path,
    defaultValue: T,
): LiteralCommandNode<CommandSourceStack> {
    return Commands.literal(literal)
        .executes { context ->
            runCatching {
                val duration = measureTime {
                    bootstrap(filePath, defaultValue)
                }
                context.source.sender.sendReloaded(literal, duration.inWholeMilliseconds)
            }.getOrElse { exception ->
                context.source.sender.sendReloadFailure(literal, exception)
                return@executes 0
            }

            Command.SINGLE_SUCCESS
        }
        .build()
}

private fun CommandSender.sendReloaded(name: String, milliseconds: Long) {
    sendMessage(
        Component.text()
            .append(Component.text("Reloaded ", NamedTextColor.GREEN))
            .append(Component.text(name, NamedTextColor.AQUA))
            .append(Component.text(" in ${milliseconds}ms.", NamedTextColor.GREEN))
            .build()
    )
}

private fun CommandSender.sendReloadFailure(name: String, exception: Throwable) {
    val reason = exception.message ?: exception::class.simpleName ?: "Unknown error"
    sendMessage(
        Component.text("Failed to reload $name: $reason", NamedTextColor.RED)
    )
}
