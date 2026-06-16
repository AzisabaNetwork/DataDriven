package net.azisaba.data.brigadier

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.azisaba.data.content.ContentHolder
import net.azisaba.data.content.Contents
import net.azisaba.data.examination.ComponentExaminer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender

private const val KEY_ARGUMENT: String = "key"

/**
 * Creates a command node that displays the details of a content entry.
 *
 * The created command accepts a content key and sends an examined representation of the value to the sender.
 *
 * @param literal the literal name of this command node.
 * @return the created command node.
 */
fun <T : Any> Contents<T>.buildDescribeCommandNode(literal: String): LiteralCommandNode<CommandSourceStack> {
    return Commands.literal(literal)
        .then(
            Commands.argument(KEY_ARGUMENT, buildHolderArgumentType())
                .executes { context ->
                    @Suppress("UNCHECKED_CAST")
                    val holder = context.getArgument(KEY_ARGUMENT, ContentHolder::class.java) as ContentHolder<T>

                    sendDescription(context.source.sender, holder)
                    Command.SINGLE_SUCCESS
                }
        )
        .build()
}

private fun <T : Any> sendDescription(commandSender: CommandSender, holder: ContentHolder<T>) {
    val value = holder.get()
    commandSender.sendMessage(
        withInfoPrefix {
            append(Component.text("Identifier: "))
            append(Component.text(holder.key.asString(), NamedTextColor.AQUA))
            appendNewline()

            append(Component.text("-".repeat(12), NamedTextColor.WHITE))
            appendNewline()

            append(ComponentExaminer.simple().examine(value))
        }
    )
}
