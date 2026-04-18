package net.azisaba.data.contents

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import kotlin.time.measureTime

fun <T : Any> DynamicContents<T>.toReloadCommand(literal: String): LiteralCommandNode<CommandSourceStack> =
    Commands.literal(literal)
        .executes { ctx ->
            val source = ctx.source

            source.sender.sendMessage(
                Component.text()
                    .color(NamedTextColor.GRAY)
                    .append(Component.text("Reloading "))
                    .append(Component.text(name, NamedTextColor.AQUA))
                    .append(Component.text("..."))
                    .build()
            )

            return@executes try {
                val duration = measureTime { reload() }
                val millis = duration.inWholeMilliseconds
                val count = all().size

                source.sender.sendMessage(
                    Component.text()
                        .color(NamedTextColor.GREEN)
                        .append(Component.text("Reloaded "))
                        .append(Component.text(name, NamedTextColor.AQUA))
                        .append(Component.text(" successfully"))
                        .append(
                            Component.text(
                                "($count entr${if (count == 1) "y" else "ies"} in ${millis}ms)",
                                NamedTextColor.GRAY,
                            )
                        )
                        .build()
                )
                Command.SINGLE_SUCCESS
            } catch (e: Exception) {
                source.sender.sendMessage(
                    Component.text()
                        .append(Component.text("Reload failed: ", NamedTextColor.RED))
                        .append(
                            Component.text(
                                e.message ?: e::class.simpleName ?: "Unknown error",
                                NamedTextColor.GRAY,
                            )
                        )
                        .build()
                )
                0
            }
        }
        .build()
