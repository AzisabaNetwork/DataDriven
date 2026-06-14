package net.azisaba.data.provider

import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import java.util.stream.Stream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

internal fun Random.nextGaussian(): Double {
    val u1 = nextDouble()
    val u2 = nextDouble()
    return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
}

/**
 * Produces numeric values, optionally using a caller-supplied random generator.
 *
 * @param T the produced number type
 */
fun interface NumberProvider<T : Number> {
    /**
     * Produces a value using [random].
     *
     * @param random the random generator to use
     * @return the produced value
     */
    fun provide(random: Random): T

    /**
     * Produces a value using [Random.Default].
     *
     * @return the produced value
     */
    fun provide(): T {
        return provide(Random.Default)
    }
}

/**
 * A base class for providers that always return the same value.
 *
 * @param T the provided number type
 */
abstract class ConstantNumberProvider<T : Number> : NumberProvider<T>, Examinable {
    /** The value returned by this provider. */
    abstract val value: T

    override fun provide(random: Random): T = value

    override fun examinableProperties(): Stream<out ExaminableProperty?> {
        return Stream.of(ExaminableProperty.of("value", value))
    }
}
