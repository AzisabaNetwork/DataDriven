package net.azisaba.data.contents

import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyPattern
import net.kyori.adventure.key.Namespaced

fun <T : Any> contentKeyOf(key: Key): ContentKey<T> {
    return ContentKeyImpl(key)
}

fun <T : Any> contentKeyOf(@KeyPattern string: String): ContentKey<T> {
    return ContentKeyImpl(Key.key(string))
}

fun <T : Any> contentKeyOf(@KeyPattern.Namespace namespace: String, @KeyPattern.Value value: String): ContentKey<T> {
    return ContentKeyImpl(Key.key(namespace, value))
}

fun <T : Any> contentKeyOf(namespaced: Namespaced, @KeyPattern.Value value: String): ContentKey<T> {
    return ContentKeyImpl(Key.key(namespaced, value))
}

sealed interface ContentKey<T : Any> : Key {
    fun resolve(contents: Contents<T>): T {
        return contents.byKeyOrThrow(this)
    }
}

private data class ContentKeyImpl<T : Any>(private val delegate: Key) : ContentKey<T> {
    override fun namespace(): String = delegate.namespace()

    override fun value(): String = delegate.value()

    override fun asString(): String = delegate.asString()

    override fun toString(): String = "ContentKey[${namespace()}:${value()}]"
}
