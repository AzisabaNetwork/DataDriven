package net.azisaba.data.contents

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

abstract class StaticContents<T : Any> : Contents<T> {
    private val byKey: ConcurrentMap<ContentKey<T>, T> = ConcurrentHashMap()
    private val byValue: ConcurrentMap<T, ContentKey<T>> = ConcurrentHashMap()

    override fun byKey(key: ContentKey<T>): T? = byKey[key]

    override fun keyOf(value: T): ContentKey<T>? = byValue[value]

    override fun all(): Collection<T> = byKey.values

    fun bootstrap() {
        val builder = BindingBuilder(byKey, byValue)
        builder.bootstrap()
    }

    protected abstract fun BindingBuilder<T>.bootstrap()

    protected class BindingBuilder<T : Any>(
        private val byKey: MutableMap<ContentKey<T>, T>,
        private val byValue: MutableMap<T, ContentKey<T>>,
    ) {
        fun <V : T> bind(key: ContentKey<T>, value: V): V {
            require(byKey.putIfAbsent(key, value) == null) {
                "Duplicate content key detected: $key"
            }
            require(byValue.putIfAbsent(value, key) == null) {
                "Duplicate value detected: $value"
            }
            return value
        }
    }
}
