plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    `java-library`
}

description = "flexboot4 - Admin Server (Spring MVC)"

dependencies {
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.aspectj:aspectjweaver")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation(libs.postgresqlJdbc)

    api(libs.mybatisFlexStarter)
    implementation(libs.mybatisFlexCodegen)
    annotationProcessor(libs.mybatisFlexProcessor)
    implementation(libs.mybatisFlexReactorSpring)
    implementation(libs.hikariCp)
    api(libs.easyexcel)

    constraints {
        implementation(libs.commonsCompress) {
            because("fix CVE-2024-25710 and CVE-2024-26308")
        }
        implementation(libs.poi) {
            because("fix CVE-2025-31672")
        }
        implementation(libs.poiOoxml)
    }

    api(libs.jjwtApi)
    runtimeOnly(libs.jjwtImpl)
    runtimeOnly(libs.jjwtJackson)

    api(libs.springdocScalarWebmvc)

    implementation(libs.ip2region)
    implementation(libs.yauaa)
    implementation(libs.oshiCore)
    api(libs.minio)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    api(project(":flexboot4-core"))
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}
