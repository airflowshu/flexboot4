plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    `java-library`
}

description = "flexboot4 - Knowledge Base Extension"

dependencies {
    api(project(":flexboot4-admin"))

    implementation(libs.pdfbox)
    implementation(libs.poiOoxml)
    // Tika might be needed if previously used, but it wasn't in admin build.gradle.
    // Assuming PDFBox and POI are enough based on file parser names.

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation(libs.mybatisFlexStarter)
    annotationProcessor(libs.mybatisFlexProcessor)
    
    // Constraints to match admin version fixes
    constraints {
        implementation(libs.commonsCompress) {
            because("fix CVE-2024-25710 and CVE-2024-26308")
        }
        implementation(libs.poi) {
            because("fix CVE-2025-31672")
        }
    }

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// KB 模块只是一个库，不是独立运行的应用，不需要打可执行 Jar 包
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}
