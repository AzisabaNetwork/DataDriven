package net.azisaba.data.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.data.Weighted
import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import java.util.stream.Stream
import kotlin.random.Random

/**
 * Selects a value from a configured set of choices.
 *
 * @param T the selected value type
 */
@Serializable
sealed interface ChoiceProvider<T : Any> : Examinable {
    /**
     * Selects a value using [random].
     *
     * @param random the random generator to use
     * @return the selected value
     */
    fun choice(random: Random = Random.Default): T

    /**
     * Always returns [value].
     *
     * @param T the selected value type
     * @property value the value to return
     */
    @Serializable
    @SerialName("Fixed")
    data class Fixed<T : Any>(val value: T) : ChoiceProvider<T> {
        override fun choice(random: Random): T = value

        override fun examinableProperties(): Stream<out ExaminableProperty?> {
            return Stream.of(ExaminableProperty.of("value", value))
        }
    }

    /**
     * Selects each configured value with equal probability.
     *
     * @param T the selected value type
     * @property values the non-empty list of values to select from
     * @throws IllegalArgumentException if [values] is empty
     */
    @Serializable
    @SerialName("Uniform")
    data class Uniform<T : Any>(val values: List<T>) : ChoiceProvider<T> {
        init {
            require(values.isNotEmpty()) { "values must not be empty" }
        }

        override fun choice(random: Random): T {
            return values[random.nextInt(values.size)]
        }

        override fun examinableProperties(): Stream<out ExaminableProperty?> {
            return Stream.of(ExaminableProperty.of("values", values))
        }
    }

    /**
     * Selects a value with probability proportional to its weight.
     *
     * @param T the selected value type
     * @property entries the non-empty list of weighted values
     * @throws IllegalArgumentException if [entries] is empty
     * @throws IllegalArgumentException if the computed total weight is not positive
     */
    @Serializable
    @SerialName("WeightedList")
    data class WeightedList<T : Any>(val entries: List<Weighted<T>>) : ChoiceProvider<T> {
        private val totalWeight: Int = entries.sumOf(Weighted<T>::weight)

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

        override fun examinableProperties(): Stream<out ExaminableProperty?> {
            return Stream.of(ExaminableProperty.of("entries", entries))
        }
    }
}
