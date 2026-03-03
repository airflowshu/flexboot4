import org.gradle.kotlin.dsl.withGroovyBuilder

plugins {
    base
    alias(libs.plugins.springDependencyManagement) apply false
}

group = "com.yunlbd"
version = "1.0.1-SNAPSHOT"

val springBootVersion = libs.versions.springBoot.get()

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version

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

    plugins.withId("io.spring.dependency-management") {
        extensions.getByName("dependencyManagement").withGroovyBuilder {
            "imports" {
                "mavenBom"(
                    "org.springframework.boot:spring-boot-dependencies:$springBootVersion"
                )
            }
        }
    }


    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        systemProperty("file.encoding", "UTF-8")
        systemProperty("user.language", "zh")
        systemProperty("user.country", "CN")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation", "-parameters"))
        options.encoding = "UTF-8"
    }
}
