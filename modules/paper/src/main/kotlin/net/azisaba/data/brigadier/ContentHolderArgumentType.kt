package net.azisaba.data.brigadier

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import net.azisaba.data.content.ContentHolder
import net.azisaba.data.content.ContentKey
import net.azisaba.data.content.Contents
import net.azisaba.data.content.contentKeyOf
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.util.concurrent.CompletableFuture

/**
 * Creates a Paper Brigadier argument type that resolves keys to content holders.
 *
 * The argument uses Paper's native key argument and suggests keys currently present in this
 * collection.
 *
 * @param T the content value type
 * @return an argument type that resolves existing content keys
 */
fun <T : Any> Contents<T>.asHolderArgumentType(): ArgumentType<ContentHolder<T>> {
    return ContentHolderArgumentTypeImpl(this)
}

private class ContentHolderArgumentTypeImpl<T : Any>(
    private val contents: Contents<T>,
) : CustomArgumentType.Converted<ContentHolder<T>, Key> {
    override fun getNativeType(): ArgumentType<Key> = ArgumentTypes.key()

    override fun convert(nativeType: Key): ContentHolder<T> {
        val key = contentKeyOf<T>(nativeType)
        if (key !in contents) {
            throw ERROR_NOT_FOUND.create(key.asString())
        }
        return ContentHolder(key, contents)
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remainingLowerCase
        contents.contentKeys()
            .asSequence()
            .filter { key ->
                key.asString().lowercase().startsWith(remaining) ||
                    key.value().lowercase().startsWith(remaining)
            }
            .sortedBy(ContentKey<T>::asString)
            .forEach { key -> builder.suggest(key.asString()) }
        return builder.buildFuture()
    }

    private companion object {
        val ERROR_NOT_FOUND: DynamicCommandExceptionType = DynamicCommandExceptionType { key ->
            MessageComponentSerializer.message().serialize(
                Component.text("Content not found: $key", NamedTextColor.RED)
            )
        }
    }
}
