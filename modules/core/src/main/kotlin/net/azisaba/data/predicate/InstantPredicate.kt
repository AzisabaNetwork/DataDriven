package net.azisaba.data.predicate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import java.util.function.Predicate
import java.util.stream.Stream
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Tests instants against a configured condition.
 */
@Serializable
sealed interface InstantPredicate : Predicate<Instant>, Examinable {
    /**
     * Accepts only [value].
     *
     * @property value the required instant
     */
    @Serializable
    @SerialName("Exact")
    data class Exact(val value: Instant) : InstantPredicate {
        override fun test(t: Instant): Boolean {
            return t == value
        }

        override fun examinableProperties(): Stream<out ExaminableProperty> {
            return Stream.of(ExaminableProperty.of("value", value))
        }
    }

    /**
     * Accepts instants before the current system time.
     *
     * The current time is read from [Clock.System] for each test.
     */
    @Serializable
    @SerialName("Past")
    data object Past : InstantPredicate {
        override fun test(t: Instant): Boolean {
            return t < Clock.System.now()
        }
    }

    /**
     * Accepts instants after the current system time.
     *
     * The current time is read from [Clock.System] for each test.
     */
    @Serializable
    @SerialName("Future")
    data object Future : InstantPredicate {
        override fun test(t: Instant): Boolean {
            return t > Clock.System.now()
        }
    }

    /**
     * Accepts instants within [tolerance] of the current system time.
     *
     * The boundary is inclusive, and the current time is read from [Clock.System] for each test.
     * A negative tolerance rejects every instant.
     *
     * @property tolerance the maximum absolute difference from the current time
     */
    @Serializable
    @SerialName("NearNow")
    data class NearNow(val tolerance: Duration) : InstantPredicate {
        override fun test(t: Instant): Boolean {
            return (t - Clock.System.now()).absoluteValue <= tolerance
        }

        override fun examinableProperties(): Stream<out ExaminableProperty> {
            return Stream.of(ExaminableProperty.of("tolerance", tolerance))
        }
    }

    /**
     * Accepts instants at or after [min].
     *
     * @property min the inclusive lower bound
     */
    @Serializable
    @SerialName("AtLeast")
    data class AtLeast(override val min: Instant) : AtLeastPredicate<Instant>(), InstantPredicate

    /**
     * Accepts instants at or before [max].
     *
     * @property max the inclusive upper bound
     */
    @Serializable
    @SerialName("AtMost")
    data class AtMost(override val max: Instant) : AtMostPredicate<Instant>(), InstantPredicate

    /**
     * Accepts instants in the inclusive range from [min] to [max].
     *
     * @property min the inclusive lower bound
     * @property max the inclusive upper bound
     */
    @Serializable
    @SerialName("Between")
    data class Between(override val min: Instant, override val max: Instant) : BetweenPredicate<Instant>(), InstantPredicate

    /**
     * Negates [predicate].
     *
     * @property predicate the predicate to negate
     */
    @Serializable
    @SerialName("Not")
    data class Not(override val predicate: InstantPredicate) : NotPredicate<Instant>(), InstantPredicate

    /**
     * Accepts an instant when any predicate in [predicates] accepts it.
     *
     * @property predicates the non-empty predicates to evaluate in list order
     * @throws IllegalArgumentException if [predicates] is empty
     */
    @Serializable
    @SerialName("Any")
    data class Any(override val predicates: List<InstantPredicate>) : AnyPredicate<Instant>(), InstantPredicate {
        init {
            require(predicates.isNotEmpty()) {
                "predicates must not be empty"
            }
        }
    }

    /**
     * Accepts an instant when every predicate in [predicates] accepts it.
     *
     * @property predicates the non-empty predicates to evaluate in list order
     * @throws IllegalArgumentException if [predicates] is empty
     */
    @Serializable
    @SerialName("All")
    data class All(override val predicates: List<InstantPredicate>) : AllPredicate<Instant>(), InstantPredicate {
        init {
            require(predicates.isNotEmpty()) {
                "predicates must not be empty"
            }
        }
    }
}
