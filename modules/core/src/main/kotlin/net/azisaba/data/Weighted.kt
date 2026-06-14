package net.azisaba.data

import kotlinx.serialization.Serializable
import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import java.util.stream.Stream
import kotlin.random.Random

/**
 * Selects a random entry from this collection with probability proportional to its weight.
 *
 * For example, an entry with weight `10` is twice as likely to be selected as an entry with weight
 * `5`.
 *
 * @param T the weighted value type
 * @param random the random generator used for the selection
 * @return the selected entry
 * @throws IllegalArgumentException if this collection is empty
 * @throws IllegalArgumentException if the total weight is not a positive [Int]
 */
fun <T : Any> Collection<Weighted<T>>.randomWeighted(random: Random = Random.Default): Weighted<T> {
    require(isNotEmpty()) {
        "Collection must not be empty"
    }

    val totalWeight = sumOf(Weighted<T>::weight)
    var target = random.nextInt(totalWeight)

    for (weighted in this) {
        target -= weighted.weight
        if (target < 0) {
            return weighted
        }
    }

    error("Unreachable")
}

/**
 * Associates [value] with a positive selection [weight].
 *
 * @param T the weighted value type
 * @property value the weighted value
 * @property weight the positive selection weight
 * @throws IllegalArgumentException if [weight] is not greater than zero
 */
@Serializable
data class Weighted<T : Any>(val value: T, val weight: Int) : Examinable {
    init {
        require(weight > 0) { "weight must be greater than 0" }
    }

    override fun examinableProperties(): Stream<out ExaminableProperty?> {
        return Stream.of(
            ExaminableProperty.of("value", value),
            ExaminableProperty.of("weight", weight),
        )
    }
}
