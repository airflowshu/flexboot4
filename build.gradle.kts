plugins {
    base
}

group = "com.yunlbd"
version = "0.0.1-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withId("java") {
        the<JavaPluginExtension>().toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }
    plugins.withId("java-library") {
        the<JavaPluginExtension>().toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
        options.encoding = "UTF-8"
    }
}
