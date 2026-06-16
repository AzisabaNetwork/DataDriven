package net.azisaba.data.brigadier

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import net.azisaba.data.content.ContentHolder
import net.azisaba.data.content.Contents
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

private const val PAGE_ARGUMENT: String = "page"
private const val QUERY_ARGUMENT: String = "query"
private const val PAGE_OFFSET: Int = 2

private val ERROR_INVALID_PAGE: DynamicCommandExceptionType = DynamicCommandExceptionType { page ->
    MessageComponentSerializer.message().serialize(
        withFailurePrefix {
            append(Component.text("Page "))
            append(Component.text(page.toString(), NamedTextColor.WHITE))
            append(Component.text(" does not exist."))
        }
    )
}

private val ERROR_EMPTY: SimpleCommandExceptionType = SimpleCommandExceptionType(
    MessageComponentSerializer.message().serialize(
        withFailurePrefix {
            append(Component.text("No contents found."))
        }
    )
)

/**
 * Creates a command node that lists content keys with simple pagination.
 *
 * The created command supports `literal`, `literal <page>`, `literal <query>`, and
 * `literal <query> <page>`. Page buttons in the footer use Adventure click callbacks,
 * so they do not depend on the final command path used by the plugin.
 *
 * @param literal the literal name of this command node.
 * @param pageSize the maximum number of entries shown per page.
 * @return the created command node.
 */
fun <T : Any> Contents<T>.buildGetCommandNode(
    literal: String,
    pageSize: Int = 10,
): LiteralCommandNode<CommandSourceStack> {
    require(pageSize > 0) {
        "pageSize must be greater than 0"
    }

    return Commands.literal(literal)
        .executes { context ->
            context.source.sender.sendPage(this, null, 1, pageSize)
        }
        .then(
            Commands.argument(PAGE_ARGUMENT, IntegerArgumentType.integer(1))
                .executes { context ->
                    val page = IntegerArgumentType.getInteger(context, PAGE_ARGUMENT)
                    context.source.sender.sendPage(this, null, page, pageSize)
                }
        )
        .then(
            Commands.argument(QUERY_ARGUMENT, StringArgumentType.word())
                .executes { context ->
                    val query = StringArgumentType.getString(context, QUERY_ARGUMENT)
                    context.source.sender.sendPage(this, query, 1, pageSize)
                }
                .then(
                    Commands.argument(PAGE_ARGUMENT, IntegerArgumentType.integer(1))
                        .executes { context ->
                            val query = StringArgumentType.getString(context, QUERY_ARGUMENT)
                            val page = IntegerArgumentType.getInteger(context, PAGE_ARGUMENT)
                            context.source.sender.sendPage(this, query, page, pageSize)
                        }
                )
        )
        .build()
}

private fun <T : Any> Audience.sendPage(contents: Contents<T>, query: String?, page: Int, pageSize: Int): Int {
    val holders = contents.filterBy(query)
    if (holders.isEmpty()) {
        throw ERROR_EMPTY.create()
    }

    val pageCount = holders.pageCount(pageSize)
    if (page > pageCount) {
        throw ERROR_INVALID_PAGE.create(page)
    }

    sendMessage(createPage(contents, holders, page, pageSize, query))
    return Command.SINGLE_SUCCESS
}

private fun <T : Any> createPage(
    contents: Contents<T>,
    holders: List<ContentHolder<T>>,
    page: Int,
    pageSize: Int,
    query: String?,
): Component {
    val pageHolders = holders.page(page, pageSize)
    val builder = Component.text()

    builder.append(buildHeaderComponent(holders, page, pageSize))
    builder.appendNewline()

    if (query != null) {
        builder.append(
            withInfoPrefix {
                append(Component.text("Filtered by "))
                append(Component.text("\"$query\"", NamedTextColor.WHITE))
                append(Component.text("."))
            }
        )
        builder.appendNewline()
    }

    for (holder in pageHolders) {
        builder.append(
            withInfoPrefix {
                append(Component.text("- ", NamedTextColor.DARK_GRAY))
                append(Component.text(holder.key.namespace()))
                append(Component.text(Key.DEFAULT_SEPARATOR, NamedTextColor.BLUE))
                append(Component.text(holder.key.value()))
            }
        )
        builder.appendNewline()
    }

    builder.append(buildFooterComponent(contents, holders, page, pageSize, query))
    return builder.build()
}

private fun <T : Any> buildHeaderComponent(
    holders: List<ContentHolder<T>>,
    page: Int,
    pageSize: Int,
): Component {
    val pageCount = holders.pageCount(pageSize)
    return withInfoPrefix {
        append(Component.text("-".repeat(9), NamedTextColor.YELLOW))
        appendSpace()
        append(Component.text("Contents: "))
        append(Component.text("($page/$pageCount)", NamedTextColor.WHITE))
        appendSpace()
        append(Component.text("-".repeat(25), NamedTextColor.YELLOW))
    }
}

private fun <T : Any> buildFooterComponent(
    contents: Contents<T>,
    holders: List<ContentHolder<T>>,
    page: Int,
    pageSize: Int,
    query: String?,
): Component {
    val firstPage = maxOf(1, page - PAGE_OFFSET)
    val lastPage = minOf(holders.pageCount(pageSize), page + PAGE_OFFSET)

    return withInfoPrefix {
        for (targetPage in firstPage..lastPage) {
            if (targetPage == page) {
                append(Component.text("[", NamedTextColor.GRAY))
                append(Component.text(targetPage, NamedTextColor.WHITE, TextDecoration.BOLD))
                append(Component.text("]", NamedTextColor.GRAY))
            } else {
                append(
                    Component.text(targetPage, NamedTextColor.AQUA)
                        .clickEvent(
                            ClickEvent.callback(
                                { audience ->
                                    audience.sendPage(contents, query, targetPage, pageSize)
                                }
                            ) { options -> options.uses(ClickCallback.UNLIMITED_USES) }
                        )
                        .hoverEvent(
                            HoverEvent.showText(
                                Component.text()
                                    .append(Component.text("Go to page "))
                                    .append(Component.text(targetPage, NamedTextColor.WHITE))
                                    .append(Component.text("."))
                                    .build()
                            )
                        )
                )
            }
            if (targetPage != lastPage) appendSpace()
        }
    }
}

private fun <T : Any> List<ContentHolder<T>>.page(page: Int, pageSize: Int): List<ContentHolder<T>> {
    val fromIndex = (page - 1) * pageSize
    return subList(fromIndex, minOf(fromIndex + pageSize, size))
}

private fun <T : Any> List<ContentHolder<T>>.pageCount(pageSize: Int): Int {
    return (size + pageSize - 1) / pageSize
}

private fun <T : Any> Contents<T>.filterBy(query: String?): List<ContentHolder<T>> {
    val sortedHolders = sortedBy { holder -> holder.key.asString() }

    return if (query != null) {
        sortedHolders.filter { holder ->
            holder.key.asString().contains(query, ignoreCase = true) ||
                    holder.key.value().contains(query, ignoreCase = true)
        }
    } else sortedHolders
}
