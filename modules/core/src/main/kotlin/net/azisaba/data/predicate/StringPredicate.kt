package net.azisaba.data.predicate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import java.util.function.Predicate
import java.util.stream.Stream

/**
 * Tests strings against a configured condition.
 */
@Serializable
sealed interface StringPredicate : Predicate<String>, Examinable {
    /**
     * Accepts strings equal to [value].
     *
     * @property value the required string
     * @property ignoreCase whether character case is ignored
     */
    @Serializable
    @SerialName("Exact")
    data class Exact(val value: String, val ignoreCase: Boolean = false) : StringPredicate {
        override fun test(t: String): Boolean {
            return t.equals(value, ignoreCase)
        }

        override fun examinableProperties(): Stream<out ExaminableProperty> {
            return Stream.of(
                ExaminableProperty.of("value", value),
                ExaminableProperty.of("ignoreCase", ignoreCase),
            )
        }
    }

    /**
     * Accepts strings containing [value].
     *
     * @property value the substring to find
     * @property ignoreCase whether character case is ignored
     */
    @Serializable
    @SerialName("Contains")
    data class Contains(val value: String, val ignoreCase: Boolean = false) : StringPredicate {
        override fun test(t: String): Boolean {
            return t.contains(value, ignoreCase)
        }

        override fun examinableProperties(): Stream<out ExaminableProperty> {
            return Stream.of(
                ExaminableProperty.of("value", value),
                ExaminableProperty.of("ignoreCase", ignoreCase),
            )
        }
    }

    /**
     * Accepts strings starting with [value].
     *
     * @property value the required prefix
     * @property ignoreCase whether character case is ignored
     */
    @Serializable
    @SerialName("StartsWith")
    data class StartsWith(val value: String, val ignoreCase: Boolean = false) : StringPredicate {
        override fun test(t: String): Boolean {
            return t.startsWith(value, ignoreCase)
        }

        override fun examinableProperties(): Stream<out ExaminableProperty> {
            return Stream.of(
                ExaminableProperty.of("value", value),
                ExaminableProperty.of("ignoreCase", ignoreCase),
            )
        }
    }

    /**
     * Accepts strings ending with [value].
     *
     * @property value the required suffix
     * @property ignoreCase whether character case is ignored
     */
    @Serializable
    @SerialName("EndsWith")
    data class EndsWith(val value: String, val ignoreCase: Boolean = false) : StringPredicate {
        override fun test(t: String): Boolean {
            return t.endsWith(value, ignoreCase)
        }

        override fun examinableProperties(): Stream<out ExaminableProperty> {
            return Stream.of(
                ExaminableProperty.of("value", value),
                ExaminableProperty.of("ignoreCase", ignoreCase),
            )
        }
    }

    /**
     * Accepts strings whose length is accepted by [predicate].
     *
     * Length is measured using [String.length], in UTF-16 code units.
     *
     * @property predicate the predicate applied to the string length
     */
    @Serializable
    @SerialName("Length")
    data class Length(val predicate: IntPredicate) : StringPredicate {
        override fun test(t: String): Boolean {
            return predicate.test(t.length)
        }

        override fun examinableProperties(): Stream<out ExaminableProperty> {
            return Stream.of(ExaminableProperty.of("predicate", predicate))
        }
    }

    /**
     * Accepts strings whose entire contents match [regex].
     *
     * The regular expression is compiled each time the predicate is tested.
     *
     * @property regex the Kotlin regular expression pattern
     * @throws java.util.regex.PatternSyntaxException when tested if [regex] is invalid
     */
    @Serializable
    @SerialName("Regex")
    data class Regex(val regex: String) : StringPredicate {
        override fun test(t: String): Boolean {
            return t.matches(regex.toRegex())
        }

        override fun examinableProperties(): Stream<out ExaminableProperty> {
            return Stream.of(ExaminableProperty.of("regex", regex))
        }
    }

    /** Accepts every string. */
    @Serializable
    @SerialName("Always")
    data object Always : AlwaysPredicate<String>(), StringPredicate

    /** Rejects every string. */
    @Serializable
    @SerialName("Never")
    data object Never : NeverPredicate<String>(), StringPredicate

    /**
     * Negates [predicate].
     *
     * @property predicate the predicate to negate
     */
    @Serializable
    @SerialName("Not")
    data class Not(override val predicate: StringPredicate) : NotPredicate<String>(), StringPredicate

    /**
     * Accepts a string when any predicate in [predicates] accepts it.
     *
     * @property predicates the non-empty predicates to evaluate in list order
     * @throws IllegalArgumentException if [predicates] is empty
     */
    @Serializable
    @SerialName("Any")
    data class Any(override val predicates: List<StringPredicate>) : AnyPredicate<String>(), StringPredicate {
        init {
            require(predicates.isNotEmpty()) {
                "predicates must not be empty"
            }
        }
    }

    /**
     * Accepts a string when every predicate in [predicates] accepts it.
     *
     * @property predicates the non-empty predicates to evaluate in list order
     * @throws IllegalArgumentException if [predicates] is empty
     */
    @Serializable
    @SerialName("All")
    data class All(override val predicates: List<StringPredicate>) : AllPredicate<String>(), StringPredicate {
        init {
            require(predicates.isNotEmpty()) {
                "predicates must not be empty"
            }
        }
    }
}
