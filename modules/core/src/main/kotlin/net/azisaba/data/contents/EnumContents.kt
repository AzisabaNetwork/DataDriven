package net.azisaba.data.contents

import net.kyori.adventure.key.Keyed
import kotlin.reflect.KClass

abstract class EnumContents<T>(kClass: KClass<T>) : Contents<T> where T : Enum<T>, T : Keyed {
    private val byKey: Map<ContentKey<T>, T>
    private val byValue: Map<T, ContentKey<T>>

    init {
        val enumConstants = kClass.java.enumConstants

        val keyMap = HashMap<ContentKey<T>, T>(enumConstants.size)
        val valueMap = HashMap<T, ContentKey<T>>(enumConstants.size)

        for (enumConstant in enumConstants) {
            val key = contentKeyOf<T>(enumConstant.key())

            require(key !in keyMap) {
                "Duplicate content key detected: $key in ${kClass.qualifiedName}"
            }

            keyMap[key] = enumConstant
            valueMap[enumConstant] = key
        }

        byKey = keyMap
        byValue = valueMap
    }

    override fun byKey(key: ContentKey<T>): T? {
        return byKey[key]
    }

    override fun keyOf(value: T): ContentKey<T>? {
        return byValue[value]
    }

    override fun contentKeys(): Collection<ContentKey<T>> {
        return byKey.keys
    }

    override fun contents(): Collection<T> {
        return byValue.keys
    }

    override fun toMap(): Map<ContentKey<T>, T> {
        return byKey
    }
}
