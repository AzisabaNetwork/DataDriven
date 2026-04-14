package net.azisaba.data.contents

import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyPattern
import net.kyori.adventure.key.Namespaced

sealed interface ContentKey<T : Any> : Key {
    companion object {
        fun <T : Any> key(@KeyPattern.Namespace namespace: String, @KeyPattern.Value value: String): ContentKey<T> =
            key(Key.key(namespace, value))

        fun <T : Any> key(namespaced: Namespaced, @KeyPattern.Value value: String): ContentKey<T> =
            key(Key.key(namespaced, value))

        fun <T : Any> key(key: Key): ContentKey<T> = ContentKeyImpl(key)
    }
}

private data class ContentKeyImpl<T : Any>(private val key: Key) : ContentKey<T> {
    override fun namespace(): String = key.namespace()

    override fun value(): String = key.value()

    override fun asString(): String = key.asString()

    override fun toString(): String = "ContentKey[${namespace()}:${value()}]"
}
