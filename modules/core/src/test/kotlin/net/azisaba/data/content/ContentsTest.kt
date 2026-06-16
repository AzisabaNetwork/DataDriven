package net.azisaba.data.content

import net.kyori.adventure.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class ContentsTest {
    @Test
    fun `register preserves a concrete generic value type`() {
        val duration: GameAttribute<Duration> = GameAttributes.DURATION.get()

        assertEquals(5.minutes, duration.value)
        assertEquals(
            duration,
            GameAttributes.byKey(contentKeyOf<GameAttribute<Duration>>("minecraft", "duration")),
        )
        assertNull(
            GameAttributes.byKey(contentKeyOf<GameAttribute<String>>("minecraft", "duration")),
        )
        assertEquals(duration, GameAttributes.byKey(Key.key("minecraft", "duration")))

        val holder = GameAttributes.holderByKeyOrThrow(contentKeyOf<GameAttribute<Duration>>("minecraft", "duration"))
        val holderByKey = GameAttributes.holderByKeyOrThrow(Key.key("minecraft", "duration"))

        assertEquals(duration, holder.get())
        assertEquals(duration, holderByKey.get())
        assertNull(GameAttributes.holderByKey(contentKeyOf<GameAttribute<String>>("minecraft", "duration")))
        assertNull(GameAttributes.byKey(Key.key("minecraft", "missing")))
        assertNull(GameAttributes.holderByKey(Key.key("minecraft", "missing")))
    }

    @Test
    fun `keyOf resolves a key by value identity`() {
        val contents = MutableContents<GameAttribute<Duration>>()
        val firstKey = contentKeyOf<GameAttribute<Duration>>("minecraft", "first")
        val secondKey = contentKeyOf<GameAttribute<Duration>>("minecraft", "second")
        val first = GameAttribute(5.minutes)
        val second = GameAttribute(5.minutes)

        contents.register(firstKey, first)
        contents.register(secondKey, second)

        assertEquals(firstKey, contents.keyOf(first))
        assertEquals(secondKey, contents.keyOf(second))
        assertNull(contents.keyOf(GameAttribute(5.minutes)))

        contents.unregister(firstKey)

        assertNull(contents.keyOf(first))
        assertEquals(secondKey, contents.keyOf(second))
    }

    private data class GameAttribute<T : Any>(val value: T)

    private companion object GameAttributes : MutableContents<GameAttribute<*>>() {
        val DURATION: ContentHolder<GameAttribute<Duration>> =
            register(contentKeyOf("minecraft", "duration"), GameAttribute(5.minutes))
    }
}
