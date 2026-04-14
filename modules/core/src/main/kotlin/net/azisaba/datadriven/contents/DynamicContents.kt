package net.azisaba.datadriven.contents

import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.*

abstract class DynamicContents<T : Any>(
    val name: String,
    private val fileExtensions: Set<String>,
    private val serializer: Lazy<KSerializer<T>>,
    private val stringFormat: StringFormat,
) : Contents<T> {
    private val cacheData: AtomicReference<CacheData<T>?> = AtomicReference(null)

    override fun byKey(key: ContentKey<T>): T? = requireLoaded().byKey[key]

    override fun keyOf(value: T): ContentKey<T>? = requireLoaded().byValue[value]

    override fun all(): Collection<T> = requireLoaded().byKey.values

    fun bootstrap(root: Path) {
        val contentsRoot = root.resolve(name)
        contentsRoot.createDirectories()

        require(contentsRoot.isDirectory()) {
            "Path is not a directory: $contentsRoot"
        }

        val byKey = HashMap<ContentKey<T>, T>()
        for (file in contentsRoot.walk().filter { it.isRegularFile() && it.hasValidExtension() }) {
            val key = file.keyFromContentsRoot(contentsRoot)
            val value = file.deserialized()

            require(key !in byKey) {
                "Duplicate content key detected in contents '$name': $key ($file)"
            }
            byKey[key] = value
        }

        val byValue = IdentityHashMap<T, ContentKey<T>>(byKey.size)
        for ((key, value) in byKey) {
            require(value !in byValue) {
                "Duplicate value detected in contents '$name': $value"
            }
            byValue[value] = key
        }

        cacheData.set(CacheData(byKey, byValue))
    }

    private fun requireLoaded(): CacheData<T> =
        cacheData.get() ?: throw IllegalStateException("You are trying to access contents '$name' too early")

    private fun Path.hasValidExtension(): Boolean = extension.isNotEmpty() && extension.lowercase() in fileExtensions

    private fun Path.keyFromContentsRoot(contentsRoot: Path): ContentKey<T> {
        val relative = relativeTo(contentsRoot)

        require(relative.nameCount >= 2) {
            "Invalid path structure (excepted namespace/value): $this"
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

        return try {
            ContentKey.key(namespace, value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid content key: $namespace:$value ($this)", e)
        }
    }

    private fun Path.deserialized(): T {
        val text = try {
            readText()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to read file $this", e)
        }

        return try {
            stringFormat.decodeFromString(serializer.value, text)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse ${stringFormat::class.simpleName} file: $this\n$text", e)
        }
    }

    private fun String.removeExtension(): String {
        val dotIndex = lastIndexOf('.')
        return if (dotIndex == -1) this else substring(0, dotIndex)
    }

    private data class CacheData<T : Any>(
        val byKey: Map<ContentKey<T>, T>,
        val byValue: Map<T, ContentKey<T>>,
    )
}
