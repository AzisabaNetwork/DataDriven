package net.azisaba.data.contents

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

fun <T : Any> Contents<T>.toGetCommand(
    literal: String,
    stringify: (T) -> String = { it.toString() },
): LiteralCommandNode<CommandSourceStack> =
    Commands.literal(literal)
        .executes { ctx ->
            val source = ctx.source

            val entries = all()
                .mapNotNull { value -> keyOf(value) }
                .sortedBy(Key::asString)
            if (entries.isEmpty()) {
                source.sender.sendMessage(Component.text("No entries found"))
                return@executes 0
            }

            source.sender.sendMessage(
                Component.text { builder ->
                    builder.appendNewline()
                    builder.append(
                        Component.text()
                            .color(NamedTextColor.GRAY)
                            .append(Component.text("Showing "))
                            .append(Component.text(entries.size, NamedTextColor.AQUA))
                            .append(Component.text(" entr${if (entries.size == 1) "y" else "ies"}:"))
                            .build()
                    )
                    entries.forEach { entry ->
                        builder.appendNewline()
                        builder.append(
                            Component.text()
                                .append(Component.text("-", NamedTextColor.DARK_GRAY))
                                .appendSpace()
                                .append(Component.text(entry.asString()))
                        )
                    }
                    builder.appendNewline()
                }
            )

            Command.SINGLE_SUCCESS
        }
        .then(
            Commands.argument("key", ArgumentTypes.key())
                .suggests { _, builder ->
                    val input = builder.remaining.lowercase()

                    all().mapNotNull { keyOf(it) }
                        .filter {
                            it.namespace().startsWith(input, ignoreCase = true) ||
                                    it.value().startsWith(input, ignoreCase = true) ||
                                    it.asString().startsWith(input, ignoreCase = true)
                        }
                        .map(Key::asString)
                        .sorted()
                        .forEach(builder::suggest)

                    builder.buildFuture()
                }
                .executes { ctx ->
                    val source = ctx.source
                    val key = ctx.getArgument("key", Key::class.java)

                    val value = byKey(ContentKey.key(key))
                    if (value == null) {
                        source.sender.sendMessage {
                            Component.text()
                                .append(Component.text("No entry found for ", NamedTextColor.RED))
                                .append(Component.text(key.asString(), NamedTextColor.GRAY))
                                .build()
                        }
                        return@executes 0
                    }

                    source.sender.sendMessage(
                        Component.text()
                            .appendNewline()
                            .append(Component.text(key.asString(), NamedTextColor.AQUA))
                            .appendNewline()
                            .append(Component.text(stringify(value), NamedTextColor.GRAY))
                            .appendNewline()
                            .build()
                    )

                    Command.SINGLE_SUCCESS
                }
        )
        .build()
