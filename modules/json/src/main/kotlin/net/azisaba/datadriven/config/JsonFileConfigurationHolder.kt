package net.azisaba.datadriven.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class JsonFileConfigurationHolder<T : Any>(serializer: KSerializer<T>) : FileConfigurationHolder<T>(
    serializer,
    stringFormat = Json {
        classDiscriminator = "Kind"
        ignoreUnknownKeys = true
    },
)
