package net.azisaba.data.content

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.azisaba.data.Holder
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyedValue
import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import java.util.stream.Stream
import kotlin.reflect.KType

/**
 * Resolves a value from [contents] using [key].
 *
 * The value is looked up each time the holder is accessed and is not cached.
 *
 * @param T the held value type
 * @property key the key used to resolve the value
 * @property contents the collection queried for the value
 */
data class ContentHolder<T : Any>(val key: ContentKey<T>, val contents: Contents<in T>) : Holder<T>, Examinable {
    override fun getOrNull(): T? {
        return contents.byKey(key)
    }

    /**
     * Resolves the current value as a keyed value.
     *
     * @return a keyed value containing [key] and the resolved value
     * @throws NoSuchElementException if [contents] has no value for [key]
     */
    fun asKeyedValue(): KeyedValue<T> {
        return KeyedValue.keyedValue(key, get())
    }

    override fun examinableProperties(): Stream<out ExaminableProperty?> {
        return Stream.of(
            ExaminableProperty.of("key", key),
            ExaminableProperty.of("contents", contents),
        )
    }
}

/**
 * Serializes a [ContentHolder] as an Adventure key string.
 *
 * Deserialized holders resolve values against [contents]. When [validateOnDeserialize] is `true`,
 * deserialization rejects keys that are not present in [contents]. When it is `false`, a missing
 * key is not detected until the holder is accessed.
 *
 * @param T the held value type
 * @param contents the collection used by deserialized holders
 * @param contentType the type assigned to deserialized keys
 * @param validateOnDeserialize whether to reject keys missing from [contents]
 */
abstract class ContentHolderSerializer<T : Any>(
    private val contents: Contents<T>,
    private val contentType: KType,
    private val validateOnDeserialize: Boolean = true,
) : KSerializer<ContentHolder<T>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ContentHolder", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ContentHolder<T>) {
        encoder.encodeString(value.key.asString())
    }

    override fun deserialize(decoder: Decoder): ContentHolder<T> {
        val contentKey: ContentKey<T> = contentKeyOf(Key.key(decoder.decodeString()), contentType)

        if (validateOnDeserialize) {
            require(contentKey in contents) {
                "Unknown content key: $contentKey"
            }
        }

        return ContentHolder(contentKey, contents)
    }
}
