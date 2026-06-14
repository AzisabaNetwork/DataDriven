package net.azisaba.data.brigadier

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.azisaba.data.content.ContentHolder
import net.azisaba.data.content.ContentKey
import net.azisaba.data.content.Contents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

private const val PAGE_OR_SEARCH_ARGUMENT = "page-or-search"

/**
 * Creates a `<literal> [page-or-search]` node for a content listing command.
 *
 * A positive integer selects a page. Any other value filters entries by a case-insensitive partial
 * match against the full key or key value. The caller is responsible for attaching the returned
 * node to the command tree.
 *
 * @param T the content value type
 * @param literal the resource literal
 * @param pageSize the number of entries displayed per page
 * @param summaryRenderer renders the summary displayed beside each key
 * @return the resource command node
 * @throws IllegalArgumentException if [pageSize] is not positive
 */
fun <T : Any> Contents<T>.asGetCommandNode(
    literal: String,
    pageSize: Int = 10,
    summaryRenderer: (ContentKey<T>, T) -> Component = { _, value -> Component.text(value.toString()) },
): LiteralCommandNode<CommandSourceStack> {
    require(pageSize > 0) {
        "pageSize must be greater than 0"
    }

    return Commands.literal(literal)
        .executes { context ->
            context.source.sendPage(this, literal, 1, pageSize, summaryRenderer)
        }
        .then(
            Commands.argument(PAGE_OR_SEARCH_ARGUMENT, StringArgumentType.greedyString())
                .executes { context ->
                    val input = StringArgumentType.getString(context, PAGE_OR_SEARCH_ARGUMENT).trim()
                    val page = input.toIntOrNull()?.takeIf { it > 0 }

                    if (input.isEmpty() || page != null) {
                        context.source.sendPage(this, literal, page ?: 1, pageSize, summaryRenderer)
                    } else {
                        context.source.sendSearch(this, literal, input, summaryRenderer)
                    }
                }
        )
        .build()
}

private fun <T : Any> CommandSourceStack.sendPage(
    contents: Contents<T>,
    resource: String,
    page: Int,
    pageSize: Int,
    summaryRenderer: (ContentKey<T>, T) -> Component,
): Int {
    val entries = contents.sortedEntries()
    if (entries.isEmpty()) {
        sender.sendMessage(Component.text("No $resource found.", NamedTextColor.YELLOW))
        return 0
    }

    val pageCount = (entries.size + pageSize - 1) / pageSize
    if (page > pageCount) {
        sender.sendMessage(
            Component.text("Page $page does not exist. Last page: $pageCount.", NamedTextColor.RED)
        )
        return 0
    }

    val fromIndex = (page - 1) * pageSize
    val pageEntries = entries.subList(fromIndex, minOf(fromIndex + pageSize, entries.size))

    sender.sendMessage(
        Component.text("$resource - page $page/$pageCount (${entries.size} total)", NamedTextColor.WHITE)
    )
    sendEntries(pageEntries, summaryRenderer)
    return Command.SINGLE_SUCCESS
}

private fun <T : Any> CommandSourceStack.sendSearch(
    contents: Contents<T>,
    resource: String,
    search: String,
    summaryRenderer: (ContentKey<T>, T) -> Component,
): Int {
    val query = search.lowercase()
    val matches = contents.sortedEntries().filter { holder ->
        holder.key.asString().lowercase().contains(query) ||
            holder.key.value().lowercase().contains(query)
    }

    if (matches.isEmpty()) {
        sender.sendMessage(
            Component.text("No $resource matching \"$search\" found.", NamedTextColor.YELLOW)
        )
        return 0
    }

    sender.sendMessage(
        Component.text("$resource matching \"$search\" (${matches.size})", NamedTextColor.WHITE)
    )
    sendEntries(matches, summaryRenderer)
    return Command.SINGLE_SUCCESS
}

private fun <T : Any> CommandSourceStack.sendEntries(
    entries: List<ContentHolder<T>>,
    summaryRenderer: (ContentKey<T>, T) -> Component,
) {
    entries.forEach { holder ->
        sender.sendMessage(
            Component.text()
                .append(Component.text(holder.key.asString(), NamedTextColor.AQUA))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(summaryRenderer(holder.key, holder.get()))
                .build()
        )
    }
}

private fun <T : Any> Contents<T>.sortedEntries(): List<ContentHolder<T>> {
    return sortedBy { holder -> holder.key.asString() }
}
