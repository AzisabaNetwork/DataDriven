package net.azisaba.data.predicate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import java.util.function.Predicate
import java.util.stream.Stream
import kotlin.time.Duration

/**
 * Tests durations against a configured condition.
 */
@Serializable
sealed interface DurationPredicate : Predicate<Duration>, Examinable {
    /**
     * Accepts only [value].
     *
     * @property value the required duration
     */
    @Serializable
    @SerialName("Exact")
    data class Exact(val value: Duration) : DurationPredicate {
        override fun test(t: Duration): Boolean {
            return t == value
        }

        override fun examinableProperties(): Stream<out ExaminableProperty> {
            return Stream.of(ExaminableProperty.of("value", value))
        }
    }

    /** Accepts positive durations. */
    @Serializable
    @SerialName("Positive")
    data object Positive : DurationPredicate {
        override fun test(t: Duration): Boolean {
            return t.isPositive()
        }
    }

    /** Accepts negative durations. */
    @Serializable
    @SerialName("Negative")
    data object Negative : DurationPredicate {
        override fun test(t: Duration): Boolean {
            return t.isNegative()
        }
    }

    /** Accepts finite durations. */
    @Serializable
    @SerialName("Finite")
    data object Finite : DurationPredicate {
        override fun test(t: Duration): Boolean {
            return t.isFinite()
        }
    }

    /** Accepts infinite durations. */
    @Serializable
    @SerialName("Infinite")
    data object Infinite : DurationPredicate {
        override fun test(t: Duration): Boolean {
            return t.isInfinite()
        }
    }

    /** Accepts [Duration.ZERO]. */
    @Serializable
    @SerialName("Zero")
    data object Zero : DurationPredicate {
        override fun test(t: Duration): Boolean {
            return t == Duration.ZERO
        }
    }

    /**
     * Accepts durations whose whole-nanosecond value is divisible by [duration].
     *
     * @property duration the positive step size
     * @throws IllegalArgumentException if [duration] is not positive
     */
    @Serializable
    @SerialName("Step")
    data class Step(val duration: Duration) : DurationPredicate {
        init {
            require(duration.isPositive()) {
                "step must be positive: $duration"
            }
        }

        override fun test(t: Duration): Boolean {
            return t.inWholeNanoseconds % duration.inWholeNanoseconds == 0L
        }

        override fun examinableProperties(): Stream<out ExaminableProperty> {
            return Stream.of(ExaminableProperty.of("duration", duration))
        }
    }

    /**
     * Accepts durations greater than or equal to [min].
     *
     * @property min the inclusive lower bound
     */
    @Serializable
    @SerialName("AtLeast")
    data class AtLeast(override val min: Duration) : AtLeastPredicate<Duration>(), DurationPredicate

    /**
     * Accepts durations less than or equal to [max].
     *
     * @property max the inclusive upper bound
     */
    @Serializable
    @SerialName("AtMost")
    data class AtMost(override val max: Duration) : AtMostPredicate<Duration>(), DurationPredicate

    /**
     * Accepts durations in the inclusive range from [min] to [max].
     *
     * @property min the inclusive lower bound
     * @property max the inclusive upper bound
     */
    @Serializable
    @SerialName("Between")
    data class Between(override val min: Duration, override val max: Duration) : BetweenPredicate<Duration>(), DurationPredicate

    /** Accepts every duration. */
    @Serializable
    @SerialName("Always")
    data object Always : AlwaysPredicate<Duration>(), DurationPredicate

    /** Rejects every duration. */
    @Serializable
    @SerialName("Never")
    data object Never : NeverPredicate<Duration>(), DurationPredicate

    /**
     * Negates [predicate].
     *
     * @property predicate the predicate to negate
     */
    @Serializable
    @SerialName("Not")
    data class Not(override val predicate: DurationPredicate) : NotPredicate<Duration>(), DurationPredicate

    /**
     * Accepts a duration when any predicate in [predicates] accepts it.
     *
     * @property predicates the non-empty predicates to evaluate in list order
     * @throws IllegalArgumentException if [predicates] is empty
     */
    @Serializable
    @SerialName("Any")
    data class Any(override val predicates: List<DurationPredicate>) : AnyPredicate<Duration>(), DurationPredicate {
        init {
            require(predicates.isNotEmpty()) {
                "predicates must not be empty"
            }
        }
    }

    /**
     * Accepts a duration when every predicate in [predicates] accepts it.
     *
     * @property predicates the non-empty predicates to evaluate in list order
     * @throws IllegalArgumentException if [predicates] is empty
     */
    @Serializable
    @SerialName("All")
    data class All(override val predicates: List<DurationPredicate>) : AllPredicate<Duration>(), DurationPredicate {
        init {
            require(predicates.isNotEmpty()) {
                "predicates must not be empty"
            }
        }
    }
}
