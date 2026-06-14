package net.azisaba.data.content

import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import net.azisaba.data.Holder
import net.kyori.adventure.key.Keyed
import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.io.path.*
import kotlin.reflect.KClass

/**
 * Stores values under unique typed keys.
 *
 * Multiple keys may be associated with equal values.
 *
 * Iteration yields [ContentHolder] instances that resolve values from this collection. Iteration
 * order is implementation-defined unless a concrete implementation specifies otherwise.
 *
 * @param T the stored value type
 *
 * @see MutableContents
 * @see EnumContents
 * @see DirectoryContents
 */
@ApiStatus.Experimental
interface Contents<T : Any> : Iterable<ContentHolder<T>>, Examinable {
    /** The number of entries in this collection. */
    val size: Int

    /**
     * Returns the value associated with the given [ContentKey].
     *
     * @param key the key to query
     * @return the associated value, or `null` if [key] is not present
     */
    fun byKey(key: ContentKey<T>): T?

    /**
     * Returns the value associated with the given [ContentKey].
     *
     * @param key the key to query
     * @return the associated value
     * @throws NoSuchElementException if [key] is not present
     */
    fun byKeyOrThrow(key: ContentKey<T>): T {
        return byKey(key) ?: throw kotlin.NoSuchElementException("No content with $key")
    }

    /**
     * Returns all values contained in this collection.
     *
     * The returned collection contains one value per key and therefore may contain equal values.
     *
     * @return a collection containing all values
     */
    fun contents(): Collection<T>

    /**
     * Returns all keys contained in this collection.
     *
     * @return a set containing all keys
     */
    fun contentKeys(): Set<ContentKey<T>>

    /**
     * Returns whether this collection contains no values.
     *
     * @return `true` if this collection is empty
     */
    fun isEmpty(): Boolean {
        return contentKeys().isEmpty()
    }

    /**
     * Returns whether this collection contains at least one value.
     *
     * @return `true` if this collection is not empty
     */
    fun isNotEmpty(): Boolean {
        return contentKeys().isNotEmpty()
    }

    /**
     * Returns whether a value exists for the given [ContentKey].
     *
     * @param key the key to test
     * @return `true` if the key is present
     */
    operator fun contains(key: ContentKey<T>): Boolean {
        return byKey(key) != null
    }

    /**
     * Returns whether the given value is contained in this collection.
     *
     * This operation may scan every entry.
     *
     * @param value the value to test
     * @return `true` if the value is present
     */
    operator fun contains(value: T): Boolean {
        return any { holder -> holder.getOrNull() == value }
    }

    override fun examinableProperties(): Stream<out ExaminableProperty> {
        return StreamSupport.stream(spliterator(), false)
            .map { holder -> ExaminableProperty.of(holder.key.asString(), holder.getOrNull()) }
    }
}

/**
 * Stores mutable content in a concurrent key index.
 *
 * Multiple keys may be registered with equal values.
 *
 * @param T the stored value type
 */
@ApiStatus.Experimental
open class MutableContents<T : Any> : Contents<T> {
    override val size: Int
        get() = byKey.size

    private val byKey: ConcurrentMap<ContentKey<T>, T> = ConcurrentHashMap()

    /**
     * Registers [value] under [key].
     *
     * @param key the key to register
     * @param value the value to register
     * @return the registered value
     * @throws IllegalArgumentException if [key] is already registered
     */
    open fun register(key: ContentKey<T>, value: T): Holder<T> {
        require(byKey.putIfAbsent(key, value) == null) {
            "Content key $key is already registered"
        }
        return ContentHolder(key, this)
    }

    /**
     * Removes the value associated with [key].
     *
     * @param key the key to remove
     * @return the removed value, or `null` if [key] is not present
     */
    open fun unregister(key: ContentKey<T>): T? {
        return byKey.remove(key)
    }

    override fun byKey(key: ContentKey<T>): T? {
        return byKey[key]
    }

    override fun contents(): Collection<T> {
        return byKey.values.toList()
    }

    override fun contentKeys(): Set<ContentKey<T>> {
        return byKey.keys.toSet()
    }

    override fun iterator(): Iterator<ContentHolder<T>> {
        return byKey.keys
            .asSequence()
            .map { key -> ContentHolder(key, this) }
            .iterator()
    }
}

/**
 * Exposes keyed constants from an enum type as contents.
 *
 * Keys are derived from each enum constant's [Keyed.key]. The contents are immutable after
 * construction.
 *
 * @param T the keyed enum type
 * @param kClass the enum class whose constants become content values
 * @throws IllegalArgumentException if multiple constants use the same key
 */
@ApiStatus.Experimental
open class EnumContents<T>(kClass: KClass<T>) : Contents<T> where T : Enum<T>, T : Keyed {
    override val size: Int = kClass.java.enumConstants.size

    private val byKey: Map<ContentKey<T>, T>

    init {
        val enumConstants = kClass.java.enumConstants

        val keyMap = HashMap<ContentKey<T>, T>(enumConstants.size)

        for (enumConstant in enumConstants) {
            val key = contentKeyOf<T>(enumConstant.key())

            require(keyMap.putIfAbsent(key, enumConstant) == null) {
                "Duplicate content key detected: $key in ${kClass.qualifiedName}"
            }
        }

        byKey = keyMap
    }

    override fun byKey(key: ContentKey<T>): T? {
        return byKey[key]
    }

    override fun contents(): Collection<T> {
        return byKey.values.toList()
    }

    override fun contentKeys(): Set<ContentKey<T>> {
        return byKey.keys
    }

    override fun iterator(): Iterator<ContentHolder<T>> {
        return byKey.keys
            .asSequence()
            .map { key -> ContentHolder(key, this) }
            .iterator()
    }
}

/**
 * Loads serialized content recursively from a directory.
 *
 * A file is loaded when its extension matches one of [extensions], ignoring case. Its first
 * relative path segment becomes the key namespace, and the remaining path without the final
 * extension becomes the key value. For example, `example/items/sword.json` maps to
 * `example:items/sword`.
 *
 * Call [bootstrap] before accessing the collection. Each subsequent bootstrap replaces the
 * previously loaded snapshot.
 *
 * @param T the stored value type
 * @param kSerializer the serializer used to decode each content file
 * @param stringFormat the format used to decode each content file
 * @param extensions the accepted file extensions without leading dots
 */
@ApiStatus.Experimental
@OptIn(ExperimentalAtomicApi::class)
open class DirectoryContents<T : Any>(
    private val kSerializer: Lazy<KSerializer<T>>,
    private val stringFormat: StringFormat,
    vararg extensions: String,
) : Contents<T> {
    override val size: Int
        get() = requireLoaded().byKey.size

    private val extensions: Set<String> = extensions.map { it.trim().lowercase() }.toSet()

    private val cacheData: AtomicReference<CacheData<T>?> = AtomicReference(null)

    /**
     * Loads all matching content files under [rootPath].
     *
     * Creates [rootPath] when it does not exist. Existing loaded contents are replaced only after
     * every matching file has been decoded and validated.
     *
     * @param rootPath the root directory to scan
     * @throws IllegalArgumentException if [rootPath] is not a directory, a file is not nested
     * under a namespace, a path is not a valid content key, or duplicate keys exist
     * @throws IllegalStateException if a content file cannot be read
     * @throws kotlinx.serialization.SerializationException if a content file cannot be decoded
     */
    open fun bootstrap(rootPath: Path) {
        require(rootPath.createDirectories().isDirectory()) {
            "Path is not a directory: $rootPath"
        }

        val byKey = HashMap<ContentKey<T>, T>()
        for (file in rootPath.walk().filter { it.isContentFile() }) {
            val key = file.asContentKey(rootPath)
            val value = file.deserialized()

            require(byKey.putIfAbsent(key, value) == null) {
                "Duplicate content key detected: $key"
            }
        }

        cacheData.store(CacheData(byKey))
    }

    override fun byKey(key: ContentKey<T>): T? {
        return requireLoaded().byKey[key]
    }

    override fun contents(): Collection<T> {
        return requireLoaded().byKey.values.toList()
    }

    override fun contentKeys(): Set<ContentKey<T>> {
        return requireLoaded().byKey.keys
    }

    override fun iterator(): Iterator<ContentHolder<T>> {
        return requireLoaded().byKey.keys
            .map { key -> ContentHolder(key, this) }
            .iterator()
    }

    private fun requireLoaded(): CacheData<T> {
        return cacheData.load() ?: throw IllegalStateException("Contents are not loaded yet")
    }

    private fun String.removeExtension(): String {
        val dotIndex = lastIndexOf('.')
        return if (dotIndex == -1) this else substring(0, dotIndex)
    }

    private fun Path.isContentFile(): Boolean {
        return isRegularFile() && extension.isNotEmpty() && extension.lowercase() in extensions
    }

    private fun Path.asContentKey(root: Path): ContentKey<T> {
        val relative = relativeTo(root)
        require(relative.nameCount >= 2) {
            "Invalid path structure (expected namespace/value): $this"
        }

        val namespace = relative.getName(0).toString()

        val value = buildString {
            for (i in 1 until relative.nameCount) {
                if (i > 1) append("/")
                val part = relative.getName(i).toString()

                if (i == relative.nameCount - 1) {
                    append(part.removeExtension())
                } else {
                    append(part)
                }
            }
        }

        return contentKeyOf(namespace, value)
    }

    private fun Path.deserialized(): T {
        val text = runCatching(::readText).getOrElse { e ->
            throw IllegalStateException("Failed to read file $this", e)
        }
        return stringFormat.decodeFromString(kSerializer.value, text)
    }

    private data class CacheData<T : Any>(val byKey: Map<ContentKey<T>, T>)
}
