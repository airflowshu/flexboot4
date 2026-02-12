plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "flexboot4"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":flexboot4-core", ":flexboot4-admin", ":flexboot4-bootstrap", ":flexboot4-ai"
    , ":flexboot4-kb", ":flexboot4-media")
