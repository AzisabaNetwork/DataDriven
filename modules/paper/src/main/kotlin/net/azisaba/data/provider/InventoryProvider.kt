package net.azisaba.data.provider

import kotlinx.serialization.Serializable
import org.bukkit.inventory.Inventory
import kotlin.random.Random

@Serializable
data class InventoryProvider(val rolls: IntProvider, val items: ChoiceProvider<ItemStackProvider>) {
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
