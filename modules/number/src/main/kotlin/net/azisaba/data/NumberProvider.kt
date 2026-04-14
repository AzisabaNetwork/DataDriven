package net.azisaba.data

import kotlin.random.Random

interface NumberProvider<T : Number> {
    fun sample(random: Random): T
}
