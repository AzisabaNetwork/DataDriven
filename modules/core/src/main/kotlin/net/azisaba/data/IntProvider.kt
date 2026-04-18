package net.azisaba.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

@Serializable
sealed interface IntProvider {
    fun sample(random: Random = Random.Default): Int

    @Serializable
    @SerialName("Constant")
    data class Constant(val value: Int) : IntProvider {
        override fun sample(random: Random): Int = value
    }

    @Serializable
    @SerialName("Uniform")
    data class Uniform(val minInclusive: Int, val maxInclusive: Int) : IntProvider {
        init {
            require(maxInclusive >= minInclusive) {
                "maxInclusive must be greater than or equal to minInclusive: [$minInclusive, $maxInclusive]"
            }
        }

        override fun sample(random: Random): Int = random.nextInt(minInclusive, maxInclusive + 1)
    }

    @Serializable
    @SerialName("BiasedToBottom")
    data class BiasedToBottom(val minInclusive: Int, val maxInclusive: Int) : IntProvider {
        init {
            require(maxInclusive >= minInclusive) {
                "maxInclusive must be greater than or equal to minInclusive: [$minInclusive, $maxInclusive]"
            }
        }

        override fun sample(random: Random): Int =
            minInclusive + random.nextInt(random.nextInt(maxInclusive - minInclusive + 1) + 1)
    }

    @Serializable
    @SerialName("Clamped")
    data class Clamped(val source: IntProvider, val minInclusive: Int, val maxInclusive: Int) : IntProvider {
        init {
            require(maxInclusive >= minInclusive) {
                "maxInclusive must be greater than or equal to minInclusive: [$minInclusive, $maxInclusive]"
            }
        }

        override fun sample(random: Random): Int = source.sample(random).coerceIn(minInclusive, maxInclusive)
    }

    @Serializable
    @SerialName("ClampedNormal")
    data class ClampedNormal(
        val mean: Float, val deviation: Float, val minInclusive: Int, val maxInclusive: Int,
    ) : IntProvider {
        init {
            require(maxInclusive >= minInclusive) {
                "maxInclusive must be greater than or equal to minInclusive: [$minInclusive, $maxInclusive]"
            }
        }

        override fun sample(random: Random): Int =
            (random.nextGaussian().toFloat() * deviation + mean)
                .coerceIn(minInclusive.toFloat(), maxInclusive.toFloat())
                .toInt()

        private fun Random.nextGaussian(): Double {
            val u1 = nextDouble()
            val u2 = nextDouble()
            return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
        }
    }

    @Serializable
    @SerialName("WeightedList")
    data class WeightedList(val distribution: List<Entry>) : IntProvider {
        init {
            require(distribution.isNotEmpty()) {
                "distribution must not be empty"
            }
            require(distribution.all { it.weight > 0 }) {
                "distribution weights must be greater than 0"
            }
        }

        override fun sample(random: Random): Int {
            val totalWeight = distribution.sumOf(Entry::weight)
            val roll = random.nextInt(totalWeight)
            var accumulatedWeight = 0
            for (entry in distribution) {
                accumulatedWeight += entry.weight
                if (roll < accumulatedWeight) {
                    return entry.provider.sample(random)
                }
            }

            return distribution.last().provider.sample(random)
        }

        @Serializable
        data class Entry(val provider: IntProvider, val weight: Int)
    }
}
