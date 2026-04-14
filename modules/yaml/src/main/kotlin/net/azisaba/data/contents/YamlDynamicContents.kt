package net.azisaba.data.contents

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.KSerializer

abstract class YamlDynamicContents<T : Any>(name: String, serializer: Lazy<KSerializer<T>>) : DynamicContents<T>(
    name,
    fileExtensions = setOf("yml", "yaml"),
    serializer,
    stringFormat = Yaml(
        configuration = YamlConfiguration(
            polymorphismStyle = PolymorphismStyle.Property,
            polymorphismPropertyName = "Kind",
        )
    ),
)
