package net.azisaba.data.provider

import kotlinx.serialization.Serializable
import net.azisaba.serialization.ItemTypeSerializer
import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import java.util.stream.Stream
import kotlin.random.Random

@Serializable
data class ItemStackProvider(
    @Serializable(with = ItemTypeSerializer::class)
    val type: ItemType,
    val count: IntProvider,
) : Examinable {
    fun create(random: Random = Random.Default): ItemStack {
        return type.createItemStack(count.provide(random))
    }

    override fun examinableProperties(): Stream<out ExaminableProperty?> {
        return Stream.of(
            ExaminableProperty.of("type", type.key().asString()),
            ExaminableProperty.of("count", count),
        )
    }
}
