plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    java
}

description = "flexboot4 - Admin Server (Spring MVC)"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.aspectj:aspectjweaver")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation(libs.postgresqlJdbc)

    implementation(libs.mybatisFlexStarter)
    implementation(libs.mybatisFlexCodegen)
    annotationProcessor(libs.mybatisFlexProcessor)
    implementation(libs.mybatisFlexReactorSpring)
    implementation(libs.hikariCp)
    implementation(libs.easyexcel)
    implementation(libs.pdfbox)

    constraints {
        implementation(libs.commonsCompress) {
            because("fix CVE-2024-25710 and CVE-2024-26308")
        }
        implementation(libs.poi) {
            because("fix CVE-2025-31672")
        }
        implementation(libs.poiOoxml)
    }

    implementation(libs.jjwtApi)
    runtimeOnly(libs.jjwtImpl)
    runtimeOnly(libs.jjwtJackson)

    implementation(libs.springdocScalarWebmvc)

    implementation(libs.ip2region)
    implementation(libs.yauaa)
    implementation(libs.minio)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation(project(":common"))
}
