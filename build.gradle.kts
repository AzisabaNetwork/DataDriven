import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
}

group = "net.azisaba.data"
version = System.getenv("VERSION") ?: "0.0.0-SNAPSHOT"

val leafProjects = subprojects.filter { it.childProjects.isEmpty() }

configure(leafProjects) {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.azisaba.net/repository/maven-snapshots/") }
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

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                groupId = group.toString()
                artifactId = project.name
                version = version.toString()
            }
        }
        repositories {
            maven {
                name = "azisaba"
                url = if (version.toString().endsWith("SNAPSHOT")) {
                    uri("https://repo.azisaba.net/repository/maven-snapshots/")
                } else {
                    uri("https://repo.azisaba.net/repository/maven-releases/")
                }
                credentials {
                    username = System.getenv("REPO_USERNAME")
                    password = System.getenv("REPO_PASSWORD")
                }
            }
        }
    }
}

tasks.register("publishAll") {
    dependsOn(
        leafProjects.map { "${it.path}:publish" }
    )
}