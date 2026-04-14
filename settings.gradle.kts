plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "data-driven"

include(":modules:core")
include(":modules:json")
include(":modules:yaml")