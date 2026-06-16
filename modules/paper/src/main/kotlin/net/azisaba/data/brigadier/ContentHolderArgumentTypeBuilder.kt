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
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.util.concurrent.CompletableFuture

private val ERROR_NOT_FOUND: DynamicCommandExceptionType = DynamicCommandExceptionType { key ->
    MessageComponentSerializer.message().serialize(
        withFailurePrefix {
            append(Component.text("Content not found: "))
            append(Component.text(key.toString(), NamedTextColor.WHITE))
        }
    )
}

/**
 * Creates a Brigadier argument type that resolves a content key into a [ContentHolder].
 *
 * The argument accepts Adventure [Key] values and suggests keys registered in this [Contents].
 *
 * @return the created argument type.
 */
fun <T : Any> Contents<T>.buildHolderArgumentType(): ArgumentType<ContentHolder<T>> {
    return ContentHolderArgumentTypeImpl(this)
}

private class ContentHolderArgumentTypeImpl<T : Any>(
    private val contents: Contents<T>,
) : CustomArgumentType.Converted<ContentHolder<T>, Key> {
    override fun getNativeType(): ArgumentType<Key> = ArgumentTypes.key()

    override fun convert(nativeType: Key): ContentHolder<T> {
        return contents.holderByKey(nativeType) ?: throw ERROR_NOT_FOUND.create(nativeType.asString())
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remainingLowerCase
        contents.keySet()
            .asSequence()
            .filter { key ->
                key.asString().lowercase().startsWith(remaining) ||
                    key.value().lowercase().startsWith(remaining)
            }
            .sortedBy(ContentKey<T>::asString)
            .forEach { key -> builder.suggest(key.asString()) }
        return builder.buildFuture()
    }
}
