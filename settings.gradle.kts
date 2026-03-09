@file:Suppress("UnstableApiUsage")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "flexboot4"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    ":flexboot4-bom",
    ":flexboot4-core",
    ":flexboot4-admin-starter",
    ":flexboot4-kb-starter",
    ":flexboot4-media-starter",
    ":flexboot4-sms4j-starter",
    ":flexboot4-cms-starter",
    ":flexboot4-bootstrap",
    ":flexboot4-ai"
)
