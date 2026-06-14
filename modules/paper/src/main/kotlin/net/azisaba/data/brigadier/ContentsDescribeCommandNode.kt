package net.azisaba.data.brigadier

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.azisaba.data.examination.ComponentExaminer
import net.azisaba.data.content.ContentHolder
import net.azisaba.data.content.ContentKey
import net.azisaba.data.content.Contents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.examination.Examinable
import org.bukkit.command.CommandSender

private const val KEY_ARGUMENT = "key"

/**
 * Creates a `<literal> <key>` node for a content description command.
 *
 * The caller is responsible for attaching the returned node to the command tree.
 *
 * @param T the content value type
 * @param literal the resource literal
 * @param detailsRenderer renders the selected value
 * @return the resource command node
 */
fun <T : Any> Contents<T>.asDescribeCommandNode(
    literal: String,
    detailsRenderer: (ContentKey<T>, T) -> Component = { _, value -> value.asDescription() },
): LiteralCommandNode<CommandSourceStack> {
    return Commands.literal(literal)
        .then(
            Commands.argument(KEY_ARGUMENT, asHolderArgumentType())
                .executes { context ->
                    @Suppress("UNCHECKED_CAST")
                    val holder = context.getArgument(KEY_ARGUMENT, ContentHolder::class.java) as ContentHolder<T>

                    context.source.sender.sendDescription(holder, detailsRenderer)
                    Command.SINGLE_SUCCESS
                }
        )
        .build()
}

private fun <T : Any> CommandSender.sendDescription(
    holder: ContentHolder<T>,
    detailsRenderer: (ContentKey<T>, T) -> Component,
) {
    val value = holder.get()
    sendMessage(
        Component.text()
            .append(field("Key", holder.key.asString()))
            .appendNewline()
            .append(field("Type", value::class.simpleName ?: value::class.java.name))
            .appendNewline()
            .append(Component.text("Value:", NamedTextColor.WHITE))
            .appendNewline()
            .append(detailsRenderer(holder.key, value))
            .build()
    )
}

private fun field(name: String, value: String): Component {
    return Component.text()
        .append(Component.text("$name:", NamedTextColor.WHITE))
        .appendSpace()
        .append(Component.text(value, NamedTextColor.GRAY))
        .build()
}

private fun Any.asDescription(): Component {
    return if (this is Examinable) {
        examine(ComponentExaminer.simple())
    } else {
        Component.text(toString())
    }
}
