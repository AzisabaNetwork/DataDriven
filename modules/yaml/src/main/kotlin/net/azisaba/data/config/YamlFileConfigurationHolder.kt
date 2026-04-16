package net.azisaba.data.config

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.KSerializer

class YamlFileConfigurationHolder<T : Any>(serializer: KSerializer<T>) : FileConfigurationHolder<T>(
    serializer,
    stringFormat = Yaml(
        configuration = YamlConfiguration(
            polymorphismStyle = PolymorphismStyle.Property,
            polymorphismPropertyName = "kind",
        ),
    ),
)
