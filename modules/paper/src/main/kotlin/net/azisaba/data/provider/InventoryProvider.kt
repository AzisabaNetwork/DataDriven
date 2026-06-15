package net.azisaba.data.provider

import kotlinx.serialization.Serializable
import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import org.bukkit.inventory.Inventory
import java.util.stream.Stream
import kotlin.random.Random

@Serializable
data class InventoryProvider(val rolls: IntProvider, val items: ChoiceProvider<ItemStackProvider>) : Examinable {
    override fun examinableProperties(): Stream<out ExaminableProperty?> {
        return Stream.of(
            ExaminableProperty.of("rolls", rolls),
            ExaminableProperty.of("items", items),
        )
    }

    fun fillInventory(inventory: Inventory, random: Random = Random.Default) {
        val rollCount = rolls.provide(random)
        val availableSlots = (0 until inventory.size).toMutableList()

        repeat(rollCount) {
            if (availableSlots.isEmpty()) return@repeat

            val itemStack = items.choice(random).create()

            val slotIndex = random.nextInt(availableSlots.size)
            val slot = availableSlots.removeAt(slotIndex)

            inventory.setItem(slot, itemStack)
        }
    }
}
