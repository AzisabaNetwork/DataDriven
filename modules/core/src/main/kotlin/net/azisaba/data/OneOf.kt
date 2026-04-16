package net.azisaba.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
sealed interface OneOf<T : Any> {
    fun choice(random: Random = Random.Default): T

    @Serializable
    @SerialName("Fixed")
    data class Fixed<T : Any>(val value: T) : OneOf<T> {
        override fun choice(random: Random): T = value
    }

    @Serializable
    @SerialName("Uniform")
    data class Uniform<T : Any>(val values: List<T>) : OneOf<T> {
        init {
            require(values.isNotEmpty()) { "values must not be empty" }
        }

        override fun choice(random: Random): T = values[random.nextInt(values.size)]
    }

    @Serializable
    @SerialName("Weighted")
    data class Weighted<T : Any>(val entries: List<Entry<T>>) : OneOf<T> {
        private val totalWeight: Int = entries.sumOf(Entry<T>::weight)

        init {
            require(entries.isNotEmpty()) { "entries must not be empty" }
            require(totalWeight > 0) { "total weight must be greater than 0" }
        }

        override fun choice(random: Random): T {
            var r = random.nextInt(totalWeight)

            for (entry in entries) {
                if (r < entry.weight) {
                    return entry.value
                }
                r -= entry.weight
            }

            return entries.last().value
        }

        @Serializable
        data class Entry<T : Any>(val value: T, val weight: Int) {
            init {
                require(weight > 0) { "weight must be greater than 0" }
            }
        }
    }
}
