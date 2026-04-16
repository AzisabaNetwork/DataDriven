package net.azisaba.data

import kotlinx.serialization.Serializable
import net.azisaba.serialization.ItemTypeSerializer
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import kotlin.random.Random

@Serializable
data class ItemStackProvider(
    @Serializable(with = ItemTypeSerializer::class)
    val type: ItemType,
    val count: IntProvider,
) {
    fun create(random: Random = Random.Default): ItemStack =
        type.createItemStack(count.sample(random))
}
