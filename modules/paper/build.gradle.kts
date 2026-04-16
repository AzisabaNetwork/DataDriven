plugins {
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    api(project(":modules:core"))
    api(libs.minecraft.serialization.paper)
    compileOnly(libs.kotlinx.serialization.core)
    compileOnly(libs.paper.api)
}