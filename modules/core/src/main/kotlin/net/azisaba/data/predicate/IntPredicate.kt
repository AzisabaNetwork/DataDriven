package net.azisaba.data.predicate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import java.util.function.Predicate
import java.util.stream.Stream

/**
 * Tests integer values against a configured condition.
 */
@Serializable
sealed interface IntPredicate : Predicate<Int>, Examinable {
    /**
     * Accepts only [value].
     *
     * @property value the required value
     */
    @Serializable
    @SerialName("Exact")
    data class Exact(val value: Int) : IntPredicate {
        override fun test(t: Int): Boolean {
            return t == value
        }

        override fun examinableProperties(): Stream<out ExaminableProperty> {
            return Stream.of(ExaminableProperty.of("value", value))
        }
    }

    /**
     * Accepts values greater than or equal to [min].
     *
     * @property min the inclusive lower bound
     */
    @Serializable
    @SerialName("AtLeast")
    data class AtLeast(override val min: Int) : AtLeastPredicate<Int>(), IntPredicate

    /**
     * Accepts values less than or equal to [max].
     *
     * @property max the inclusive upper bound
     */
    @Serializable
    @SerialName("AtMost")
    data class AtMost(override val max: Int) : AtMostPredicate<Int>(), IntPredicate

    /**
     * Accepts values in the inclusive range from [min] to [max].
     *
     * @property min the inclusive lower bound
     * @property max the inclusive upper bound
     */
    @Serializable
    @SerialName("Between")
    data class Between(override val min: Int, override val max: Int) : BetweenPredicate<Int>(), IntPredicate

    /** Accepts every integer. */
    @Serializable
    @SerialName("Always")
    data object Always : AlwaysPredicate<Int>(), IntPredicate

    /** Rejects every integer. */
    @Serializable
    @SerialName("Never")
    data object Never : NeverPredicate<Int>(), IntPredicate

    /**
     * Negates [predicate].
     *
     * @property predicate the predicate to negate
     */
    @Serializable
    @SerialName("Not")
    data class Not(override val predicate: IntPredicate) : NotPredicate<Int>(), IntPredicate

    /**
     * Accepts a value when any predicate in [predicates] accepts it.
     *
     * @property predicates the non-empty predicates to evaluate in list order
     * @throws IllegalArgumentException if [predicates] is empty
     */
    @Serializable
    @SerialName("Any")
    data class Any(override val predicates: List<IntPredicate>) : AnyPredicate<Int>(), IntPredicate {
        init {
            require(predicates.isNotEmpty()) {
                "predicates must not be empty"
            }
        }
    }

    /**
     * Accepts a value when every predicate in [predicates] accepts it.
     *
     * @property predicates the non-empty predicates to evaluate in list order
     * @throws IllegalArgumentException if [predicates] is empty
     */
    @Serializable
    @SerialName("All")
    data class All(override val predicates: List<IntPredicate>) : AllPredicate<Int>(), IntPredicate {
        init {
            require(predicates.isNotEmpty()) {
                "predicates must not be empty"
            }
        }
    }
}
