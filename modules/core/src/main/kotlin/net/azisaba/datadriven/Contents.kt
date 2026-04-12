package net.azisaba.datadriven

interface Contents<T : Any> {
    fun byKey(key: ContentKey<T>): T?

    fun byKeyOrThrow(key: ContentKey<T>): T = byKey(key) ?: throw NoSuchElementException("No content with $key")

    fun keyOf(value: T): ContentKey<T>?

    fun keyOfOrThrow(value: T): ContentKey<T> = keyOf(value) ?: throw NoSuchElementException("No content with $value")

    fun all(): Collection<T>
}
