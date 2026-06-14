package net.azisaba.data.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.data.Weighted
import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import java.util.stream.Stream
import kotlin.random.Random

/**
 * Produces integer values from a configured distribution.
 */
@Serializable
sealed interface IntProvider : NumberProvider<Int>, Examinable {
    /**
     * Always returns [value].
     *
     * @property value the value to return
     */
    @Serializable
    @SerialName("Constant")
    data class Constant(override val value: Int) : ConstantNumberProvider<Int>(), IntProvider

    /**
     * Produces uniformly distributed values in the inclusive range from [minInclusive] to
     * [maxInclusive].
     *
     * @property minInclusive the inclusive lower bound
     * @property maxInclusive the inclusive upper bound
     * @throws IllegalArgumentException if [maxInclusive] is less than [minInclusive]
     */
    @Serializable
    @SerialName("Uniform")
    data class Uniform(val minInclusive: Int, val maxInclusive: Int) : IntProvider {
        init {
            require(maxInclusive >= minInclusive) {
                "maxInclusive must be greater than or equal to minInclusive: [$minInclusive, $maxInclusive"
            }
        }

        override fun provide(random: Random): Int {
            return random.nextInt(minInclusive, maxInclusive + 1)
        }

        override fun examinableProperties(): Stream<out ExaminableProperty?> {
            return Stream.of(
                ExaminableProperty.of("minInclusive", minInclusive),
                ExaminableProperty.of("maxInclusive", maxInclusive),
            )
        }
    }

    /**
     * Produces values biased toward [minInclusive].
     *
     * @property minInclusive the inclusive lower bound
     * @property maxInclusive the inclusive upper bound
     * @throws IllegalArgumentException if [maxInclusive] is less than [minInclusive]
     */
    @SerialName("BiasedToBottom")
    data class BiasedToBottom(val minInclusive: Int, val maxInclusive: Int) : IntProvider {
        override fun provide(random: Random): Int {
            return minInclusive + random.nextInt(random.nextInt(maxInclusive - minInclusive + 1) + 1)
        }

        override fun examinableProperties(): Stream<out ExaminableProperty?> {
            return Stream.of(
                ExaminableProperty.of("minInclusive", minInclusive),
                ExaminableProperty.of("maxInclusive", maxInclusive),
            )
        }
    }

    /**
     * Clamps values from [source] to the inclusive range from [minInclusive] to [maxInclusive].
     *
     * @property source the provider whose values are clamped
     * @property minInclusive the inclusive lower clamp bound
     * @property maxInclusive the inclusive upper clamp bound
     * @throws IllegalArgumentException if [maxInclusive] is less than [minInclusive]
     */
    @SerialName("Clamped")
    data class Clamped(val source: IntProvider, val minInclusive: Int, val maxInclusive: Int) : IntProvider {
        init {
            require(maxInclusive >= minInclusive) {
                "maxInclusive must be greater than or equal to minInclusive: [$minInclusive, $maxInclusive"
            }
        }

        override fun provide(random: Random): Int {
            return source.provide(random).coerceIn(minInclusive, maxInclusive)
        }

        override fun examinableProperties(): Stream<out ExaminableProperty?> {
            return Stream.of(
                ExaminableProperty.of("source", source),
                ExaminableProperty.of("minInclusive", minInclusive),
                ExaminableProperty.of("maxInclusive", maxInclusive),
            )
        }
    }

    /**
     * Produces normally distributed values clamped to the inclusive range from [minInclusive] to
     * [maxInclusive].
     *
     * The clamped floating-point result is converted to an integer by truncating toward zero.
     *
     * @property mean the mean of the normal distribution
     * @property deviation the standard deviation multiplier
     * @property minInclusive the inclusive lower clamp bound
     * @property maxInclusive the inclusive upper clamp bound
     * @throws IllegalArgumentException if [maxInclusive] is less than [minInclusive]
     */
    @SerialName("ClampedNormal")
    data class ClampedNormal(
        val mean: Int, val deviation: Int, val minInclusive: Int, val maxInclusive: Int,
    ) : IntProvider {
        init {
            require(maxInclusive >= minInclusive) {
                "maxInclusive must be greater than or equal to minInclusive: [$minInclusive, $maxInclusive"
            }
        }

        override fun provide(random: Random): Int {
            return (random.nextGaussian().toFloat() * deviation + mean)
                .coerceIn(minInclusive.toFloat(), maxInclusive.toFloat())
                .toInt()
        }

        override fun examinableProperties(): Stream<out ExaminableProperty?> {
            return Stream.of(
                ExaminableProperty.of("mean", mean),
                ExaminableProperty.of("deviation", deviation),
                ExaminableProperty.of("minInclusive", minInclusive),
                ExaminableProperty.of("maxInclusive", maxInclusive),
            )
        }
    }

    /**
     * Selects a provider by weight and returns the value it produces.
     *
     * @property distribution the non-empty weighted provider list
     * @throws IllegalArgumentException if [distribution] is empty
     * @throws IllegalArgumentException when producing a value if the computed total weight is not
     * positive
     */
    @SerialName("WeightedList")
    data class WeightedList(val distribution: List<Weighted<IntProvider>>) : IntProvider {
        init {
            require(distribution.isNotEmpty()) {
                "distribution must not be empty"
            }
            require(distribution.all { it.weight > 0 }) {
                "distribution weights must be positive"
            }
        }

        override fun provide(random: Random): Int {
            val totalWeight = distribution.sumOf(Weighted<IntProvider>::weight)
            val roll = random.nextInt(totalWeight)
            var accumulatedWeight = 0
            for (entry in distribution) {
                accumulatedWeight += entry.weight
                if (roll < accumulatedWeight) {
                    return entry.value.provide(random)
                }
            }

            return distribution.last().value.provide(random)
        }

        override fun examinableProperties(): Stream<out ExaminableProperty?> {
            return Stream.of(ExaminableProperty.of("distribution", distribution))
        }
    }
}
