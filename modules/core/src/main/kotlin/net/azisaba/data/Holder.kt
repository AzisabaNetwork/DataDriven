package net.azisaba.data

/**
 * Creates a holder that always returns [value].
 *
 * @param T the held value type
 * @param value the value to hold
 * @return a holder containing [value]
 */
fun <T : Any> Holder(value: T): Holder<T> = Holder { value }

/**
 * Returns the value if available, or the result of [defaultValue] otherwise.
 *
 * @param defaultValue the fallback value provider
 * @return the held value, or the fallback value
 */
inline fun <T : Any> Holder<T>.getOrElse(defaultValue: () -> T): T {
    return getOrNull() ?: defaultValue()
}

/**
 * Provides a value that may be computed, loaded lazily, or absent.
 *
 * @param T the provided value type
 */
fun interface Holder<T : Any> {
    /**
     * Returns the value.
     *
     * @return the held value
     * @throws NoSuchElementException if no value is available
     */
    fun get(): T {
        return getOrNull() ?: throw NoSuchElementException("Content value is not present")
    }

    /**
     * Returns the value if available.
     *
     * @return the held value, or `null` if no value is available
     */
    fun getOrNull(): T?

    /**
     * Returns the value if available, or [defaultValue] otherwise.
     *
     * @param defaultValue the fallback value
     * @return the held value, or the fallback value
     */
    fun getOrDefault(defaultValue: T): T {
        return getOrNull() ?: defaultValue
    }

    /**
     * Returns whether [get] completes successfully.
     *
     * This method returns `false` if [get] throws for any reason.
     *
     * @return `true` if [get] completes successfully
     */
    fun isPresent(): Boolean {
        return getOrNull() != null
    }

    /**
     * Returns a holder that applies [transform] to this holder's value.
     *
     * The returned holder is empty when this holder is empty. The transformation is evaluated each
     * time the returned holder is accessed.
     *
     * @param transform the transformation to apply
     * @return a holder that provides the transformed value
     */
    fun <R : Any> map(transform: (T) -> R): Holder<R> = Holder {
        getOrNull()?.let(transform)
    }
}
