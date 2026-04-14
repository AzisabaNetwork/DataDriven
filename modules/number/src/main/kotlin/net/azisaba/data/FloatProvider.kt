package net.azisaba.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

@Serializable
sealed interface FloatProvider : NumberProvider<Float> {
    @Serializable
    @SerialName("Constant")
    data class Constant(val value: Float) : FloatProvider {
        override fun sample(random: Random): Float = value
    }

    @Serializable
    @SerialName("Uniform")
    data class Uniform(val minInclusive: Float, val maxExclusive: Float) : FloatProvider {
        init {
            require(maxExclusive > minInclusive) {
                "maxExclusive must be greater than minInclusive: [$minInclusive, $maxExclusive]"
            }
        }

        override fun sample(random: Random): Float = random.nextFloat() * (maxExclusive - minInclusive) + minInclusive
    }

    @Serializable
    @SerialName("ClampedNormal")
    data class ClampedNormal(
        val mean: Float,
        val deviation: Float,
        val min: Float,
        val max: Float,
    ) : FloatProvider {
        init {
            require(max >= min) {
                "max must be greater than or equal to min: [$min, $max]"
            }
        }

        override fun sample(random: Random): Float =
            (random.nextGaussian().toFloat() * deviation + mean).coerceIn(min, max)

        private fun Random.nextGaussian(): Double {
            val u1 = nextDouble()
            val u2 = nextDouble()
            return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
        }
    }

    @Serializable
    @SerialName("Trapezoid")
    data class Trapezoid(val min: Float, val max: Float, val plateau: Float) : FloatProvider {
        init {
            require(max >= min) {
                "max must be greater than or equal to min: [$min, $max]"
            }
            require(plateau <= max - min) {
                "plateau must be less than or equal to the full span: [$min, $max]"
            }
        }

        override fun sample(random: Random): Float {
            val span = max - min
            val slope = (span - plateau) / 2.0f
            val base = span - slope
            return min + random.nextFloat() * base + random.nextFloat() * slope
        }
    }

    @Serializable
    @SerialName("Multiplied")
    data class Multiplied(val values: List<FloatProvider>) : FloatProvider {
        init {
            require(values.isNotEmpty()) {
                "values must not be empty"
            }
        }

        override fun sample(random: Random): Float {
            var product = 1.0f
            for (value in values) {
                product *= value.sample(random)
            }
            return product
        }
    }
}
