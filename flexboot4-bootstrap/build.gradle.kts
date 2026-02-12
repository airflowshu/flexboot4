plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    java
}

description = "flexboot4 - Bootstrap (Admin + KB Assembly)"

dependencies {
    // 引入 KB 模块，会自动传递引入 admin 和 core
    implementation(project(":flexboot4-kb"))
    // 引入 media模块，则会自动穿衣admin 和core
    // admin 会被传递引入 （一般不会重复冲突，Gradle 会做依赖图合并）
    implementation(project(":flexboot4-media"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}
