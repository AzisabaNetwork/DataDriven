plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    compileOnly(libs.kotlinx.serialization.core)
    compileOnly(libs.adventure.api)
}
