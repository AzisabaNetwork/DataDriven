package net.azisaba.datadriven.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class FileConfigurationHolder<T : Any>(
    val serializer: KSerializer<T>, val stringFormat: StringFormat,
) : ConfigurationHolder<T> {
    private val config: AtomicReference<T?> = AtomicReference(null)

    override fun config(): T =
        config.get() ?: throw IllegalStateException("You are trying to access configuration too early")

    fun bootstrap(filePath: Path, default: T) {
        if (!filePath.exists()) {
            val defaultString = stringFormat.encodeToString(serializer, default)
            filePath.parent?.let(Path::createDirectories)
            filePath.writeText(
                defaultString,
                options = arrayOf(
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE,
                ),
            )
        }

        val fileString = filePath.readText()
        val config = stringFormat.decodeFromString(serializer, fileString)

        this.config.set(config)
    }
}
