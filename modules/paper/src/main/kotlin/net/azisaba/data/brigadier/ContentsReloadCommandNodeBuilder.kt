package net.azisaba.data.brigadier

import com.mojang.brigadier.Command
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import net.azisaba.data.Holder
import net.azisaba.data.content.DirectoryContents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import java.nio.file.Path
import kotlin.time.measureTime

private val ERROR_FAILED_TO_RELOAD: DynamicCommandExceptionType = DynamicCommandExceptionType { reason ->
    MessageComponentSerializer.message().serialize(
        withFailurePrefix {
            append(Component.text("Failed to reload contents: "))
            append(Component.text(reason.toString(), NamedTextColor.WHITE))
        }
    )
}

/**
 * Creates a command node that reloads this directory-backed contents from the given root path.
 *
 * @param literal the literal name of this command node.
 * @param rootPath the root path holder used to bootstrap this contents.
 * @return the created command node.
 */
fun <T : Any> DirectoryContents<T>.buildReloadCommandNode(
    literal: String,
    rootPath: Holder<Path>,
): LiteralCommandNode<CommandSourceStack> {
    return Commands.literal(literal)
        .executes { context ->
            tryReload(this, rootPath.get(), context.source.sender)
            Command.SINGLE_SUCCESS
        }
        .build()
}

private fun <T : Any> tryReload(contents: DirectoryContents<T>, filePath: Path, commandSender: CommandSender) {
    runCatching {
        val duration = measureTime {
            contents.bootstrap(filePath)
        }
        commandSender.sendMessage(
            withSuccessPrefix {
                append(Component.text("Reloaded contents from"))
                append(Component.text(filePath.toString(), NamedTextColor.WHITE))
                append(Component.text(" in "))
                append(Component.text("${duration.inWholeMilliseconds}ms", NamedTextColor.AQUA))
                append(Component.text("."))
            }
        )
    }.onFailure {
        val reason = it.message ?: it::class.simpleName ?: "Unknown Error"
        throw ERROR_FAILED_TO_RELOAD.create(reason)
    }
}
