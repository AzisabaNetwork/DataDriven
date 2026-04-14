package net.azisaba.data.contents

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

abstract class JsonDynamicContents<T : Any>(name: String, serializer: Lazy<KSerializer<T>>) : DynamicContents<T>(
    name,
    fileExtensions = setOf("json"),
    serializer,
    stringFormat = Json {
        classDiscriminator = "Kind"
        ignoreUnknownKeys = true
    },
)
