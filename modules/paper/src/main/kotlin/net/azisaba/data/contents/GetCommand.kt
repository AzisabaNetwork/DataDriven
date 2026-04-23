package net.azisaba.data.contents

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
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
        Commands.argument("key", toArgumentType())
            .executes { ctx ->
                val source = ctx.source

                @Suppress("UNCHECKED_CAST")
                val content = ctx.getArgument("key", Any::class.java) as T
                val contentKey = keyOfOrThrow(content)

                source.sender.sendMessage(
                    Component.text()
                        .color(CommandColors.BASE)
                        .append(Component.text("ℹ", CommandColors.HIGHLIGHT))
                        .appendSpace()
                        .append(Component.text("'"))
                        .append(Component.text(contentKey.asString(), CommandColors.HIGHLIGHT))
                        .append(Component.text("'"))
                        .appendNewline()
                        .append(Component.text(stringify(content)))
                        .build()
                )

                Command.SINGLE_SUCCESS
            }
    )
    .build()
