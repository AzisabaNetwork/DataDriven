package net.azisaba.data.contents

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import kotlin.time.measureTime

fun <T : Any> DynamicContents<T>.toReloadCommand(literal: String): LiteralCommandNode<CommandSourceStack> =
    Commands.literal(literal)
        .executes { ctx ->
            val source = ctx.source

            source.sender.sendMessage(
                Component.text()
                    .color(CommandColors.BASE)
                    .append(Component.text("ℹ", CommandColors.HIGHLIGHT))
                    .appendSpace()
                    .append(Component.text("Reloading '"))
                    .append(Component.text(name, CommandColors.HIGHLIGHT))
                    .append(Component.text("'..."))
                    .build()
            )

            return@executes try {
                val duration = measureTime { reload() }

                val contentCount = contents().size

                source.sender.sendMessage(
                    Component.text()
                        .color(CommandColors.INFO)
                        .append(Component.text("Reloaded '"))
                        .append(Component.text(name, CommandColors.HIGHLIGHT))
                        .append(Component.text("' successfully"))
                        .appendNewline()
                        .append(
                            Component.text()
                                .color(CommandColors.BASE)
                                .append(Component.text("("))
                                .appendSpace()
                                .append(Component.text(contentCount, CommandColors.INFO))
                                .appendSpace()
                                .append(Component.text("content${if (contentCount > 1) "s" else ""} in"))
                                .appendSpace()
                                .append(Component.text(duration.inWholeMilliseconds, CommandColors.INFO))
                                .append(Component.text("ms"))
                                .appendSpace()
                                .append(Component.text(")"))
                                .build()
                        )
                        .build()
                )
                Command.SINGLE_SUCCESS
            } catch (e: Exception) {
                source.sender.sendMessage(
                    Component.text()
                        .append(Component.text("Reload failed: ", CommandColors.ERROR))
                        .append(
                            Component.text(
                                e.message ?: e::class.simpleName ?: "Unknown error",
                                CommandColors.WARN,
                            )
                        )
                        .build()
                )
                0
            }
        }
        .build()
