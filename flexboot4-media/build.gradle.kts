plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    `java-library`
}

description = "flexboot4 - Media Extension"

dependencies {
    api(project(":flexboot4-admin"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}
