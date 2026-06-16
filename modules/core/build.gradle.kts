plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.minecraft.serialization.adventure)
    implementation(kotlin("reflect"))
    compileOnly(libs.kotlinx.serialization.core)
    compileOnly(libs.adventure.api)
    testImplementation(libs.adventure.api)
}
