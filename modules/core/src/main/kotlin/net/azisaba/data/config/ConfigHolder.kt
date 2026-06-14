package net.azisaba.data.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import net.azisaba.data.Holder
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Holds configuration data decoded from a file.
 *
 * Call [bootstrap] before accessing the value. Each subsequent call to [bootstrap] replaces the
 * currently held value.
 *
 * @param T the configuration value type
 * @property kSerializer the serializer used to encode and decode the configuration
 * @property stringFormat the format used to read and write the configuration
 */
@OptIn(ExperimentalAtomicApi::class)
class ConfigHolder<T : Any>(val kSerializer: KSerializer<T>, val stringFormat: StringFormat) : Holder<T> {
    private val reference: AtomicReference<T?> = AtomicReference(null)

    override fun getOrNull(): T? {
        return reference.load()
    }

    /**
     * Creates the configuration file if necessary and loads its value.
     *
     * If [filePath] does not exist, this method creates its parent directories and writes
     * [defaultValue] before loading the file. Existing files are never overwritten with the
     * default value.
     *
     * @param filePath the configuration file to load
     * @param defaultValue the value to write when the file does not exist
     * @throws java.io.IOException if the file cannot be read or written
     * @throws kotlinx.serialization.SerializationException if encoding or decoding fails
     */
    fun bootstrap(filePath: Path, defaultValue: T) {
        if (!filePath.exists()) {
            val defaultString = stringFormat.encodeToString(kSerializer, defaultValue)
            filePath.parent?.let(Path::createDirectories)
            filePath.writeText(
                defaultString,
                options = arrayOf(
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE,
                )
            )
        }

        val fileString = filePath.readText()

        val value = stringFormat.decodeFromString(kSerializer, fileString)

        this.reference.store(value)
    }
}
