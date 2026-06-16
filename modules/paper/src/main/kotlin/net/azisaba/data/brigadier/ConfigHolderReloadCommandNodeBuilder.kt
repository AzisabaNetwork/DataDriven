package net.azisaba.data.brigadier

import com.mojang.brigadier.Command
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import net.azisaba.data.config.ConfigHolder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import java.nio.file.Path
import kotlin.time.measureTime

private val ERROR_FAILED_TO_RELOAD: Dynamic2CommandExceptionType = Dynamic2CommandExceptionType { path, reason ->
    MessageComponentSerializer.message().serialize(
        withFailurePrefix {
            append(Component.text("Failed to reload "))
            append(Component.text(path.toString(), NamedTextColor.WHITE))
            append(Component.text(": "))
            append(Component.text("$reason", NamedTextColor.RED))
        }
    )
}

/**
 * Creates a command node that reloads this config holder from the given file path.
 *
 * @param literal the literal name of this command node.
 * @param filePath the config file path used to bootstrap this holder.
 * @param defaultValue the value used when the config file does not exist yet.
 * @return the created command node.
 */
fun <T : Any> ConfigHolder<T>.buildReloadCommandNode(
    literal: String,
    filePath: Path,
    defaultValue: T,
): LiteralCommandNode<CommandSourceStack> {
    return Commands.literal(literal)
        .executes { context ->
            tryReload(this, filePath, defaultValue, context.source.sender)
            Command.SINGLE_SUCCESS
        }
        .build()
}

private fun <T : Any> tryReload(
    configHolder: ConfigHolder<T>,
    filePath: Path,
    defaultValue: T,
    commandSender: CommandSender,
) {
    runCatching {
        val duration = measureTime {
            configHolder.bootstrap(filePath, defaultValue)
        }
        commandSender.sendMessage(
            withSuccessPrefix {
                append(Component.text("Reloaded "))
                append(Component.text(filePath.toString(), NamedTextColor.WHITE))
                append(Component.text(" in "))
                append(Component.text("${duration.inWholeMilliseconds}ms", NamedTextColor.AQUA))
                append(Component.text("."))
            }
        )
    }.getOrElse { exception ->
        val reason = exception.message ?: exception::class.simpleName ?: "Unknown Error"
        throw ERROR_FAILED_TO_RELOAD.create(filePath, reason)
    }
}
