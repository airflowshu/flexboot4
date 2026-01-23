plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "flexboot4"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":admin-server", ":ai-gateway", ":common")
