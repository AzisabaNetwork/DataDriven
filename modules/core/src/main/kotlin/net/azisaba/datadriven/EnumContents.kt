package net.azisaba.datadriven

import net.kyori.adventure.key.Keyed
import kotlin.reflect.KClass

abstract class EnumContents<T>(kClass: KClass<T>) : Contents<T> where T : Enum<T>, T : Keyed {
    private val byKey: Map<ContentKey<T>, T>
    private val byValue: Map<T, ContentKey<T>>

    init {
        val constants = kClass.java.enumConstants

        val keyMap = HashMap<ContentKey<T>, T>(constants.size)
        val valueMap = HashMap<T, ContentKey<T>>(constants.size)

        for (constant in constants) {
            val key = ContentKey.key<T>(constant.key())

            require(key !in keyMap) {
                "Duplicate content key detected: $key in ${kClass.qualifiedName}"
            }

            keyMap[key] = constant
            valueMap[constant] = key
        }

        byKey = keyMap
        byValue = valueMap
    }

    override fun byKey(key: ContentKey<T>): T? = byKey[key]

    override fun keyOf(value: T): ContentKey<T>? = byValue[value]

    override fun all(): Collection<T> = byValue.keys
}
