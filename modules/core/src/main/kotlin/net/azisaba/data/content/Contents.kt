package net.azisaba.data.content

import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Keyed
import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.io.path.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

/**
 * Represents a collection of content values.
 *
 * A content collection guarantees one value for each key and one key for each value instance.
 * Value instances are compared by identity (`===`), not by equality. Registered keys may identify
 * a subtype of [T]; [ContentKey] is covariant so those keys can still be exposed as
 * [ContentKey]<[T]>.
 *
 * Iteration yields [ContentHolder] instances that resolve values from this collection. Iteration
 * order is defined by the implementation's index.
 *
 * @param T the stored value type
 *
 * @see IndexContents
 * @see MutableContents
 * @see EnumContents
 * @see DirectoryContents
 */
@ApiStatus.Experimental
interface Contents<T : Any> : Iterable<ContentHolder<T>>, Examinable {
    /** The number of contents in this collection. */
    val size: Int

    /**
     * Returns the value associated with [key].
     *
     * @param key the key to query
     * @return the associated value, or `null` if [key] is not present
     */
    fun byKey(key: Key): T?

    /**
     * Returns the value associated with [key].
     *
     * @param key the key to query
     * @return the associated value, or `null` if [key] is not present or its type does not match
     */
    fun <R : T> byKey(key: ContentKey<R>): R?

    /**
     * Returns the value associated with [key].
     *
     * @param key the key to query
     * @return the associated value
     * @throws NoSuchElementException if [key] is not present
     */
    fun byKeyOrThrow(key: Key): T {
        return byKey(key) ?: throw NoSuchElementException("No content with $key")
    }

    /**
     * Returns the value associated with [key].
     *
     * @param key the key to query
     * @return the associated value
     * @throws NoSuchElementException if [key] is not present or its type does not match
     */
    fun <R : T> byKeyOrThrow(key: ContentKey<R>): R {
        return byKey(key) ?: throw NoSuchElementException("No content with $key")
    }

    /**
     * Returns a holder that resolves the value associated with [key].
     *
     * The registered [ContentKey] determines the holder's value type.
     *
     * @param key the key to query
     * @return a holder for the associated value, or `null` if [key] is not present
     */
    fun holderByKey(key: Key): ContentHolder<T>?

    /**
     * Returns a holder that resolves the value associated with [key].
     *
     * @param key the key to query
     * @return a holder for the associated value, or `null` if [key] is not present or its type does not match
     */
    fun <R : T> holderByKey(key: ContentKey<R>): ContentHolder<R>? {
        return if (byKey(key) != null) ContentHolder(key, this) else null
    }

    /**
     * Returns a holder that resolves the value associated with [key].
     *
     * @param key the key to query
     * @return a holder for the associated value
     * @throws NoSuchElementException if [key] is not present
     */
    fun holderByKeyOrThrow(key: Key): ContentHolder<T> {
        return holderByKey(key) ?: throw NoSuchElementException("No content with $key")
    }

    /**
     * Returns a holder that resolves the value associated with [key].
     *
     * @param key the key to query
     * @return a holder for the associated value
     * @throws NoSuchElementException if [key] is not present or its type does not match
     */
    fun <R : T> holderByKeyOrThrow(key: ContentKey<R>): ContentHolder<R> {
        return holderByKey(key) ?: throw NoSuchElementException("No content with $key")
    }

    /**
     * Returns the key associated with [value].
     *
     * This operation compares value instances by identity (`===`), not by equality.
     *
     * @param value the value instance to query
     * @return the key associated with [value], or `null` if this collection does not contain it
     */
    fun keyOf(value: T): ContentKey<T>?

    /**
     * Returns the key associated with [value].
     *
     * This operation compares value instances by identity (`===`), not by equality.
     *
     * @param value the value instance to query
     * @return the key associated with [value]
     * @throws NoSuchElementException if this collection does not contain [value]
     */
    fun keyOfOrThrow(value: T): ContentKey<T> {
        return keyOf(value) ?: throw NoSuchElementException("No content with $value")
    }

    /**
     * Returns all values contained in this collection.
     *
     * The returned collection contains one value per key. Equal values may appear multiple times
     * when they are different instances.
     *
     * @return a collection containing all values
     */
    fun values(): Collection<T>

    /**
     * Returns all keys contained in this collection.
     *
     * @return a set containing all keys
     */
    fun keySet(): Set<ContentKey<T>>

    /**
     * Returns whether this collection contains no values.
     *
     * @return `true` if this collection is empty
     */
    fun isEmpty(): Boolean {
        return size == 0
    }

    /**
     * Returns whether this collection contains at least one value.
     *
     * @return `true` if this collection is not empty
     */
    fun isNotEmpty(): Boolean {
        return !isEmpty()
    }

    /**
     * Returns whether a value exists for the given [Key].
     *
     * @param key the key to test
     * @return `true` if the key is present
     */
    operator fun contains(key: Key): Boolean {
        return byKey(key) != null
    }

    /**
     * Returns whether a value exists for the given [ContentKey].
     *
     * @param key the key to test
     * @return `true` if the key is present and its type matches
     */
    operator fun <R : T> contains(key: ContentKey<R>): Boolean {
        return byKey(key) != null
    }

    /**
     * Returns whether the given value is contained in this collection.
     *
     * This operation compares value instances by identity (`===`), not by equality.
     *
     * @param value the value to test
     * @return `true` if the value is present
     */
    operator fun contains(value: T): Boolean {
        return keyOf(value) != null
    }

    override fun iterator(): Iterator<ContentHolder<T>>

    override fun examinableProperties(): Stream<out ExaminableProperty> {
        return StreamSupport.stream(spliterator(), false)
            .map { holder -> ExaminableProperty.of(holder.key.asString(), holder.getOrNull()) }
    }

    /**
     * Represents an immutable lookup index for a content collection.
     *
     * An index stores the derived lookup state for a content collection. It guarantees one entry for
     * each key and one entry for each value instance. Value-to-key lookup uses [IdentityHashMap], so
     * values are compared by identity (`===`), not by equality.
     *
     * @param T the indexed value type
     * @see IndexContents
     */
    class Index<T : Any> private constructor(
        private val entriesByKey: Map<Key, Entry<T>>,
        private val entriesByValue: Map<T, Entry<T>>,
    ) {
        /** The number of indexed contents. */
        val size: Int
            get() = entriesByKey.size

        /**
         * Returns the value associated with [key].
         *
         * @param key the key to query
         * @return the associated value, or `null` if [key] is not present
         */
        fun valueByKey(key: Key): T? {
            return entriesByKey[key]?.value
        }

        /**
         * Returns the value associated with [key].
         *
         * @param key the key to query
         * @return the associated value, or `null` if [key] is not present or its type does not match
         */
        fun <R : T> valueByKey(key: ContentKey<R>): R? {
            return entriesByKey[key.key()]?.valueAs(key)
        }

        /**
         * Returns the registered content key for [key].
         *
         * @param key the key to query
         * @return the registered content key, or `null` if [key] is not present
         */
        fun keyByKey(key: Key): ContentKey<T>? {
            return entriesByKey[key]?.key
        }

        /**
         * Returns the key associated with [value].
         *
         * @param value the value instance to query
         * @return the key associated with [value], or `null` if [value] is not indexed
         */
        fun keyByValue(value: T): ContentKey<T>? {
            return entriesByValue[value]?.key
        }

        /**
         * Returns all indexed values.
         *
         * @return a collection containing one value per key
         */
        fun values(): Collection<T> {
            return entriesByKey.values.map(Entry<T>::value)
        }

        /**
         * Returns all indexed content keys.
         *
         * @return a set containing all keys
         */
        fun keySet(): Set<ContentKey<T>> {
            return entriesByKey.values.mapTo(LinkedHashSet()) { entry -> entry.key }
        }

        /**
         * Creates an iterator over holders for [contents].
         *
         * @param contents the contents used by the returned holders
         * @return an iterator over holders backed by this index
         */
        fun holders(contents: Contents<T>): Iterator<ContentHolder<T>> {
            return entriesByKey.values
                .asSequence()
                .map { entry -> ContentHolder(entry.key, contents) }
                .iterator()
        }

        /**
         * Returns a new index with [key] associated with [value].
         *
         * @param key the key to add
         * @param value the value to add
         * @return the new index
         * @throws IllegalArgumentException if [key] or [value] is already present
         */
        fun <R : T> with(key: ContentKey<R>, value: R): Index<T> {
            require(key.key() !in entriesByKey) {
                "Content key $key is already registered"
            }
            require(value !in entriesByValue) {
                "Content value instance is already registered: $value"
            }
            val entry = Entry<T>(key, value)
            return Index(
                entriesByKey + (key.key() to entry),
                IdentityHashMap(entriesByValue).apply {
                    put(value, entry)
                },
            )
        }

        /**
         * Returns a new index without [key].
         *
         * @param key the key to remove
         * @return the new index
         */
        fun without(key: ContentKey<T>): Index<T> {
            val entry = entriesByKey[key.key()] ?: return this
            return Index(
                entriesByKey - key.key(),
                IdentityHashMap(entriesByValue).apply {
                    remove(entry.value)
                },
            )
        }

        companion object {
            /**
             * Creates an index from a key-value map.
             *
             * @param map the key-value map to index
             * @throws IllegalArgumentException if duplicate keys are present
             * @throws IllegalArgumentException if the same value instance is associated with multiple keys
             */
            fun <T : Any> from(map: Map<ContentKey<T>, T>): Index<T> {
                val entriesByKey = LinkedHashMap<Key, Entry<T>>(map.size)
                val entriesByValue = IdentityHashMap<T, Entry<T>>(map.size)

                for ((key, value) in map) {
                    val entry = Entry(key, value)
                    require(entriesByKey.put(entry.key.key(), entry) == null) {
                        "Duplicate content key detected: ${entry.key}"
                    }
                    require(entriesByValue.put(value, entry) == null) {
                        "Duplicate content value instance detected: $value"
                    }
                }

                return Index(entriesByKey, entriesByValue)
            }

            /**
             * Creates an empty index.
             */
            fun <T : Any> empty(): Index<T> {
                return Index(emptyMap(), emptyMap())
            }
        }

        private data class Entry<T : Any>(val key: ContentKey<T>, val value: T) {
            fun <R : T> valueAs(key: ContentKey<R>): R? {
                if (!this.key.kType.isSubtypeOf(key.kType)) {
                    return null
                }
                @Suppress("UNCHECKED_CAST")
                return value as R
            }
        }
    }
}

/**
 * Represents a base implementation backed by a [Contents.Index].
 *
 * This implementation delegates lookup, iteration, and value-to-key resolution to the current
 * [index]. Subclasses define how that index is stored and replaced.
 *
 * @param T the stored value type
 * @see Contents.Index
 */
@ApiStatus.Experimental
abstract class IndexContents<T : Any> : Contents<T> {
    override val size: Int
        get() = index.size

    protected abstract val index: Contents.Index<T>

    override fun byKey(key: Key): T? {
        return index.valueByKey(key)
    }

    override fun <R : T> byKey(key: ContentKey<R>): R? {
        return index.valueByKey(key)
    }

    override fun holderByKey(key: Key): ContentHolder<T>? {
        return index.keyByKey(key)?.let { ContentHolder(it, this) }
    }

    override fun keyOf(value: T): ContentKey<T>? {
        return index.keyByValue(value)
    }

    override fun values(): Collection<T> {
        return index.values()
    }

    override fun keySet(): Set<ContentKey<T>> {
        return index.keySet()
    }

    override fun iterator(): Iterator<ContentHolder<T>> {
        return index.holders(this)
    }
}

/**
 * Represents a mutable content collection.
 *
 * This implementation stores contents as immutable index snapshots. Registering or unregistering a
 * value creates a new snapshot and atomically replaces the current one.
 *
 * @param T the stored value type
 */
@ApiStatus.Experimental
@OptIn(ExperimentalAtomicApi::class)
open class MutableContents<T : Any> : IndexContents<T>() {
    override val index: Contents.Index<T>
        get() = indexReference.load()

    private val indexReference: AtomicReference<Contents.Index<T>> = AtomicReference(Contents.Index.empty())

    /**
     * Registers [value] under [key].
     *
     * @param key the key to register
     * @param value the value to register
     * @return the registered holder
     * @throws IllegalArgumentException if [key] or [value] is already registered
     */
    open fun <R : T> register(key: ContentKey<R>, value: R): ContentHolder<R> {
        while (true) {
            val currentIndex = indexReference.load()
            val nextIndex = currentIndex.with(key, value)
            if (indexReference.compareAndSet(currentIndex, nextIndex)) {
                return ContentHolder(key, this)
            }
        }
    }

    /**
     * Unregisters the value associated with [key].
     *
     * @param key the key to remove
     * @return the removed value, or `null` if [key] is not present or its type does not match
     */
    open fun <R : T> unregister(key: ContentKey<R>): R? {
        while (true) {
            val currentIndex = indexReference.load()
            val value = currentIndex.valueByKey(key) ?: return null
            val nextIndex = currentIndex.without(key)
            if (indexReference.compareAndSet(currentIndex, nextIndex)) {
                return value
            }
        }
    }
}

/**
 * Represents an immutable content collection backed by enum constants.
 *
 * This implementation indexes all enum constants during construction. Each key is derived from the
 * corresponding constant's [Keyed.key].
 *
 * @param T the keyed enum type
 * @param kClass the enum class whose constants become content values
 * @throws IllegalArgumentException if multiple constants use the same key
 */
@ApiStatus.Experimental
open class EnumContents<T>(kClass: KClass<T>) : IndexContents<T>() where T : Enum<T>, T : Keyed {
    override val index: Contents.Index<T> = Contents.Index.from(
        kClass.java.enumConstants.associateBy { enumConstant ->
            contentKeyOf(enumConstant.key(), kClass.createType())
        }
    )
}

/**
 * Represents a reloadable content collection backed by files in a directory.
 *
 * This implementation builds a new index from files under a root directory. A file is loaded when
 * its extension matches one of [extensions], ignoring case. Its first relative path segment becomes
 * the key namespace, and the remaining path without the final extension becomes the key value. For
 * example, `example/items/sword.json` maps to `example:items/sword`.
 *
 * Call [bootstrap] before accessing the collection. Each successful bootstrap atomically replaces
 * the current index snapshot.
 *
 * @param T the stored value type
 * @param kSerializer the serializer used to decode each content file
 * @param stringFormat the format used to decode each content file
 * @param extensions the accepted file extensions without leading dots
 */
@ApiStatus.Experimental
@OptIn(ExperimentalAtomicApi::class)
open class DirectoryContents<T : Any>(
    private val kType: KType,
    private val kSerializer: Lazy<KSerializer<T>>,
    private val stringFormat: StringFormat,
    extensions: Set<String>,
) : IndexContents<T>() {
    constructor(
        kClass: KClass<T>,
        kSerializer: Lazy<KSerializer<T>>,
        stringFormat: StringFormat,
        extensions: Set<String>,
    ) : this(kClass.createType(), kSerializer, stringFormat, extensions)

    final override val index: Contents.Index<T>
        get() = requireLoaded()

    private val extensions: Set<String> = extensions.map { it.trim().lowercase() }.toSet()

    private val indexReference: AtomicReference<Contents.Index<T>?> = AtomicReference(null)

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

        val contents = rootPath.walk()
            .filter { path -> path.isContentFile() }
            .associate { file -> file.asContentKey(rootPath) to file.deserialized() }

        indexReference.store(Contents.Index.from(contents))
    }

    private fun requireLoaded(): Contents.Index<T> {
        return indexReference.load() ?: throw IllegalStateException("Contents are not loaded yet")
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

        return contentKeyOf(Key.key(namespace, value), kType)
    }

    private fun Path.deserialized(): T {
        val text = runCatching(::readText).getOrElse { e ->
            throw IllegalStateException("Failed to read file $this", e)
        }
        return stringFormat.decodeFromString(kSerializer.value, text)
    }
}
