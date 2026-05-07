package net.azisaba.data.contents

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.key.Key

interface Contents<T : Any> {
    fun byKey(key: ContentKey<T>): T?

    fun byKeyOrThrow(key: ContentKey<T>): T = byKey(key) ?: throw NoSuchElementException("No content with $key")

    fun keyOf(value: T): ContentKey<T>?

    fun keyOfOrThrow(value: T): ContentKey<T> = keyOf(value) ?: throw NoSuchElementException("No content with $value")

    fun contentKeys(): Collection<ContentKey<T>>

    fun contents(): Collection<T>

    fun toMap(): Map<ContentKey<T>, T>
}

interface ReloadableContents<T : Any> : Contents<T> {
    val name: String

    fun reload()
}

open class ContentReferenceSerializer<T : Any>(val contents: Contents<T>) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ContentReference", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): T {
        val string = decoder.decodeString()
        val contentKey = contentKeyOf<T>(Key.key(string))
        return contents.byKeyOrThrow(contentKey)
    }
}
