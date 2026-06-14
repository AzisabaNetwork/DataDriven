package net.azisaba.data.predicate

import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import java.util.function.Predicate
import java.util.stream.Stream

/**
 * A predicate that accepts every value.
 *
 * @param T the tested value type
 */
abstract class AlwaysPredicate<T> : Predicate<T>, Examinable {
    override fun test(t: T): Boolean = true
}

/**
 * A predicate that rejects every value.
 *
 * @param T the tested value type
 */
abstract class NeverPredicate<T> : Predicate<T>, Examinable {
    override fun test(t: T): Boolean = false
}

/**
 * A predicate that negates [predicate].
 *
 * @param T the tested value type
 */
abstract class NotPredicate<T> : Predicate<T>, Examinable {
    /** The predicate to negate. */
    abstract val predicate: Predicate<T>

    override fun test(t: T): Boolean {
        return !predicate.test(t)
    }

    override fun examinableProperties(): Stream<out ExaminableProperty> {
        return Stream.of(ExaminableProperty.of("predicate", predicate))
    }
}

/**
 * A predicate that accepts a value when any predicate in [predicates] accepts it.
 *
 * An empty list rejects every value.
 *
 * @param T the tested value type
 */
abstract class AnyPredicate<T> : Predicate<T>, Examinable {
    /** The predicates to evaluate in list order. */
    abstract val predicates: List<Predicate<T>>

    override fun test(t: T): Boolean {
        return predicates.any { it.test(t) }
    }

    override fun examinableProperties(): Stream<out ExaminableProperty> {
        return Stream.of(ExaminableProperty.of("predicates", predicates))
    }
}

/**
 * A predicate that accepts a value when every predicate in [predicates] accepts it.
 *
 * An empty list accepts every value.
 *
 * @param T the tested value type
 */
abstract class AllPredicate<T> : Predicate<T>, Examinable {
    /** The predicates to evaluate in list order. */
    abstract val predicates: List<Predicate<T>>

    override fun test(t: T): Boolean {
        return predicates.all { it.test(t) }
    }

    override fun examinableProperties(): Stream<out ExaminableProperty> {
        return Stream.of(ExaminableProperty.of("predicates", predicates))
    }
}
