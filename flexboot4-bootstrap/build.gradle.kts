plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    java
}

description = "flexboot4 - Bootstrap (Internal Assembly: Admin + KB + Media)"

dependencies {
    // 引入各个 starter 模块
    implementation(project(":flexboot4-admin-starter"))
    implementation(project(":flexboot4-kb-starter"))
    implementation(project(":flexboot4-media-starter"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}
