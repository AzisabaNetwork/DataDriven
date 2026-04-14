import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
}

group = "net.azisaba.data"
version = "1.0-SNAPSHOT"

configure(subprojects.filter { it.childProjects.isEmpty() }) {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    dependencies {
        add("testImplementation", kotlin("test"))
    }

    configure<KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }
}