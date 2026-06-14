package net.azisaba.data.brigadier

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.azisaba.data.content.DirectoryContents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import java.nio.file.Path
import kotlin.time.measureTime

/**
 * Creates a command node that reloads this directory-backed content collection.
 *
 * @param T the content value type
 * @param literal the command literal
 * @param rootPath the directory to reload
 * @return the reload command node
 */
fun <T : Any> DirectoryContents<T>.asReloadCommandNode(
    literal: String,
    rootPath: Path,
): LiteralCommandNode<CommandSourceStack> {
    return Commands.literal(literal)
        .executes { context ->
            runCatching {
                val duration = measureTime {
                    bootstrap(rootPath)
                }
                context.source.sender.sendReloaded(literal, size, duration.inWholeMilliseconds)
            }.getOrElse { exception ->
                context.source.sender.sendReloadFailure(literal, exception)
                return@executes 0
            }

            Command.SINGLE_SUCCESS
        }
        .build()
}

private fun CommandSender.sendReloaded(name: String, count: Int, milliseconds: Long) {
    sendMessage(
        Component.text()
            .append(Component.text("Reloaded ", NamedTextColor.GREEN))
            .append(Component.text(name, NamedTextColor.AQUA))
            .append(Component.text(" ($count entries) in ${milliseconds}ms.", NamedTextColor.GREEN))
            .build()
    )
}

private fun CommandSender.sendReloadFailure(name: String, exception: Throwable) {
    val reason = exception.message ?: exception::class.simpleName ?: "Unknown error"
    sendMessage(
        Component.text("Failed to reload $name: $reason", NamedTextColor.RED)
    )
}
