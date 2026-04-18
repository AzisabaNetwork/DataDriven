package net.azisaba.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface IntPredicate {
    fun matches(value: Int): Boolean

    @Serializable
    @SerialName("Exact")
    data class Exact(val value: Int) : IntPredicate {
        override fun matches(value: Int): Boolean = this.value == value
    }

    @Serializable
    @SerialName("AtLeast")
    data class AtLeast(val min: Int): IntPredicate {
        override fun matches(value: Int): Boolean = value >= min
    }

    @Serializable
    @SerialName("AtMost")
    data class AtMost(val max: Int): IntPredicate {
        override fun matches(value: Int): Boolean = value <= max
    }

    @Serializable
    @SerialName("Between")
    data class Between(val min: Int, val max: Int): IntPredicate {
        override fun matches(value: Int): Boolean = value in min..max
    }

    @Serializable
    @SerialName("Always")
    object Always : IntPredicate {
        override fun matches(value: Int): Boolean = true
    }

    @Serializable
    @SerialName("Never")
    object Never : IntPredicate {
        override fun matches(value: Int): Boolean = false
    }

    @Serializable
    @SerialName("Not")
    data class Not(val predicate: IntPredicate): IntPredicate {
        override fun matches(value: Int): Boolean = !predicate.matches(value)
    }

    @Serializable
    @SerialName("AnyOf")
    data class AnyOf(val predicates: List<IntPredicate>): IntPredicate {
        init {
            require(predicates.isNotEmpty()) { "predicates cannot be empty" }
        }

        override fun matches(value: Int): Boolean = predicates.any { it.matches(value) }
    }

    @Serializable
    @SerialName("AllOf")
    data class AllOf(val predicates: List<IntPredicate>): IntPredicate {
        init {
            require(predicates.isNotEmpty()) { "predicates cannot be empty" }
        }

        override fun matches(value: Int): Boolean = predicates.all { it.matches(value) }
    }
}
