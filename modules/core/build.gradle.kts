plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    compileOnly(libs.kotlinx.serialization.core)
    compileOnly(libs.kotlinx.serialization.json)
    compileOnly(libs.adventure.api)
    compileOnly(libs.kaml)
}
