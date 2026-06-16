package net.azisaba.data.content

import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyPattern
import net.kyori.adventure.key.Namespaced
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Creates a content key from [key].
 *
 * @param T the identified value type
 * @param key the Adventure key to wrap
 * @return a content key with the same namespace and value
 */
inline fun <reified T : Any> contentKeyOf(key: Key): ContentKey<T> {
    return contentKeyOf(key, typeOf<T>())
}

/**
 * Creates a content key from [namespace] and [value].
 *
 * @param T the identified value type
 * @param namespace the namespace
 * @param value the key value
 * @return the created content key
 * @throws IllegalArgumentException if [namespace] or [value] is invalid
 */
inline fun <reified T : Any> contentKeyOf(
    @KeyPattern.Namespace namespace: String,
    @KeyPattern.Value value: String,
): ContentKey<T> {
    return contentKeyOf(Key.key(namespace, value), typeOf<T>())
}

/**
 * Creates a content key from [namespaced]'s namespace and [value].
 *
 * @param T the identified value type
 * @param namespaced the object that supplies the namespace
 * @param value the key value
 * @return the created content key
 * @throws IllegalArgumentException if the namespace or [value] is invalid
 */
inline fun <reified T : Any> contentKeyOf(
    namespaced: Namespaced,
    @KeyPattern.Value value: String,
): ContentKey<T> {
    return contentKeyOf(Key.key(namespaced, value), typeOf<T>())
}

@PublishedApi
internal fun <T : Any> contentKeyOf(key: Key, type: KType): ContentKey<T> {
    return ContentKeyImpl(key, type)
}

/**
 * Resolves this key against a collection of its type or a supertype.
 *
 * @param T the collection value type
 * @param R the value type represented by this key
 * @param contents the collection to query
 * @return the associated value
 * @throws NoSuchElementException if [contents] has no value for this key
 */
fun <T : Any, R : T> ContentKey<R>.resolve(contents: Contents<T>): R {
    return contents.byKeyOrThrow(this)
}

/**
 * Identifies a value of type [T] in a [Contents] collection.
 *
 * @param T the identified value type
 */
sealed interface ContentKey<out T : Any> : Key {
    /** The type represented by this key. */
    val kType: KType

    override fun key(): Key {
        return Key.key(namespace(), value())
    }
}

private data class ContentKeyImpl<T : Any>(private val key: Key, override val kType: KType) : ContentKey<T> {
    override fun namespace(): String = key.namespace()

    override fun value(): String = key.value()

    override fun asString(): String = key.asString()

    override fun toString(): String = "ContentKey[${namespace()}:${value()}]"
}
