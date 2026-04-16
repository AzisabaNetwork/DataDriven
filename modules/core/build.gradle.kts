plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.minecraft.serialization.adventure)
    compileOnly(libs.kotlinx.serialization.core)
    compileOnly(libs.adventure.api)
}