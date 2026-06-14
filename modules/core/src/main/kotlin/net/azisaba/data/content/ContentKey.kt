package net.azisaba.data.content

import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyPattern
import net.kyori.adventure.key.Namespaced

/**
 * Creates a content key from [key].
 *
 * @param T the identified value type
 * @param key the Adventure key to wrap
 * @return a content key with the same namespace and value
 */
fun <T : Any> contentKeyOf(key: Key): ContentKey<T> {
    return ContentKeyImpl(key)
}

/**
 * Creates a content key from an Adventure key string.
 *
 * @param T the identified value type
 * @param string the key in `namespace:value` form
 * @return the parsed content key
 * @throws IllegalArgumentException if [string] is not a valid Adventure key
 */
fun <T : Any> contentKeyOf(@KeyPattern string: String): ContentKey<T> {
    return ContentKeyImpl(Key.key(string))
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
fun <T : Any> contentKeyOf(namespace: String, @KeyPattern value: String): ContentKey<T> {
    return ContentKeyImpl(Key.key(namespace, value))
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
fun <T : Any> contentKeyOf(namespaced: Namespaced, @KeyPattern value: String): ContentKey<T> {
    return ContentKeyImpl(Key.key(namespaced, value))
}

/**
 * Identifies a value of type [T] in a [Contents] collection.
 *
 * @param T the identified value type
 */
sealed interface ContentKey<T : Any> : Key {
    /**
     * Resolves this key against [contents].
     *
     * @param contents the collection to query
     * @return the associated value
     * @throws NoSuchElementException if [contents] has no value for this key
     *
     * @see [contentKeyOf]
     * @see [Contents.byKey]
     * @see [Contents.byKeyOrThrow]
     */
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
