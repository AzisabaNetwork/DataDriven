package net.azisaba.data.predicate

import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import java.util.function.Predicate
import java.util.stream.Stream

/**
 * A predicate that accepts values greater than or equal to [min].
 *
 * @param T the compared value type
 */
abstract class AtLeastPredicate<T : Comparable<T>> : Predicate<T>, Examinable {
    /** The inclusive lower bound. */
    abstract val min: T

    override fun test(t: T): Boolean {
        return t >= min
    }

    override fun examinableProperties(): Stream<out ExaminableProperty> {
        return Stream.of(ExaminableProperty.of("min", min))
    }
}

/**
 * A predicate that accepts values less than or equal to [max].
 *
 * @param T the compared value type
 */
abstract class AtMostPredicate<T : Comparable<T>> : Predicate<T>, Examinable {
    /** The inclusive upper bound. */
    abstract val max: T

    override fun test(t: T): Boolean {
        return t <= max
    }

    override fun examinableProperties(): Stream<out ExaminableProperty> {
        return Stream.of(ExaminableProperty.of("max", max))
    }
}

/**
 * A predicate that accepts values in the inclusive range from [min] to [max].
 *
 * @param T the compared value type
 */
abstract class BetweenPredicate<T : Comparable<T>> : Predicate<T>, Examinable {
    /** The inclusive lower bound. */
    abstract val min: T

    /** The inclusive upper bound. */
    abstract val max: T

    override fun test(t: T): Boolean {
        return t in min..max
    }

    override fun examinableProperties(): Stream<out ExaminableProperty> {
        return Stream.of(
            ExaminableProperty.of("min", min),
            ExaminableProperty.of("max", max),
        )
    }
}
