package net.azisaba.data.contents

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import java.util.concurrent.CompletableFuture

fun <T : Any> Contents<T>.toArgumentType(): ArgumentType<T> = ContentArgumentType(this)

private class ContentArgumentType<T : Any>(private val contents: Contents<T>) : CustomArgumentType<T, Key> {
    override fun getNativeType(): ArgumentType<Key> = ArgumentTypes.key()

    override fun parse(reader: StringReader): T {
        val key = nativeType.parse(reader)
        val contentKey = contentKeyOf<T>(key)
        val content = contents.byKey(contentKey) ?: throw ERROR_NOT_FOUND.create(contentKey.asString())
        return content
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        contents.contentKeys()
            .filter { contentKey ->
                contentKey.asString().startsWith(builder.remainingLowerCase) ||
                        contentKey.value().startsWith(builder.remainingLowerCase)
            }
            .map(ContentKey<T>::asString)
            .forEach(builder::suggest)
        return builder.buildFuture()
    }

    companion object {
        val ERROR_NOT_FOUND: DynamicCommandExceptionType = DynamicCommandExceptionType { key ->
            MessageComponentSerializer.message().serialize(Component.text("Unknown content: '$key'"))
        }
    }
}
