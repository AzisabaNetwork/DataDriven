package net.azisaba.data.contents

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor

fun <T : Any> Contents<T>.toGetCommand(
    literal: String,
    stringify: (T) -> String = { it.toString() },
): LiteralCommandNode<CommandSourceStack> = Commands.literal(literal)
    .executes { ctx ->
        val source = ctx.source

        val contentKeys = contentKeys().sortedBy(ContentKey<T>::asString)
        if (contentKeys.isEmpty()) {
            source.sender.sendMessage(Component.text("No contents found", CommandColors.WARN))
            return@executes 0
        }

        source.sender.sendMessage(
            Component.text { builder ->
                builder.append(
                    Component.text()
                        .color(CommandColors.BASE)
                        .append(Component.text("ℹ", CommandColors.HIGHLIGHT))
                        .appendSpace()
                        .append(Component.text("Showing "))
                        .append(Component.text(contentKeys.size, CommandColors.INFO))
                        .append(Component.text(" content${if (contentKeys.size > 1) "s" else ""}:"))
                        .build()
                )
                contentKeys.forEach { contentKey ->
                    builder.appendNewline()
                    builder.append(
                        Component.text()
                            .appendSpace()
                            .append(Component.text("-", CommandColors.INFO))
                            .appendSpace()
                            .append(
                                Component.text(contentKey.asString(), NamedTextColor.GRAY)
                                    .hoverEvent(
                                        HoverEvent.showText(
                                            Component.text(stringify(byKey(contentKey)!!), CommandColors.BASE)
                                        )
                                    )
                            )
                    )
                }
            }
        )

        Command.SINGLE_SUCCESS
    }
    .then(
        Commands.argument("key", ArgumentTypes.key())
            .suggests { _, builder ->
                val input = builder.remaining.lowercase()

                contentKeys().filter {
                    it.namespace().startsWith(input, ignoreCase = true) ||
                            it.value().startsWith(input, ignoreCase = true) ||
                            it.asString().startsWith(input, ignoreCase = true)
                }.map(Key::asString).sorted().forEach(builder::suggest)

                builder.buildFuture()
            }
            .executes { ctx ->
                val source = ctx.source
                val key = ctx.getArgument("key", Key::class.java)

                val value = byKey(ContentKey.key(key))
                if (value == null) {
                    source.sender.sendMessage {
                        Component.text()
                            .append(Component.text("No content found for ", CommandColors.ERROR))
                            .append(Component.text(key.asString(), NamedTextColor.GRAY))
                            .build()
                    }
                    return@executes 0
                }

                source.sender.sendMessage(
                    Component.text()
                        .color(CommandColors.BASE)
                        .append(Component.text("ℹ", CommandColors.HIGHLIGHT))
                        .appendSpace()
                        .append(Component.text("'"))
                        .append(Component.text(key.asString(), CommandColors.HIGHLIGHT))
                        .append(Component.text("'"))
                        .appendNewline()
                        .append(Component.text(stringify(value)))
                        .build()
                )

                Command.SINGLE_SUCCESS
            }
    )
    .build()
