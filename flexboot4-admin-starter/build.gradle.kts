plugins {
    alias(libs.plugins.springDependencyManagement)
    `java-library`
    `maven-publish`
}

description = "flexboot4 - Admin Starter (RBAC + Base Features)"

dependencies {
    api(project(":flexboot4-admin-kernel"))

    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.springframework.boot:spring-boot-starter-jdbc")
    api("org.springframework.boot:spring-boot-starter-flyway")
    api("org.springframework.boot:spring-boot-starter-mail")

    api("org.aspectj:aspectjweaver")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor(libs.mapstructProcessor)

    api(libs.postgresqlJdbc)
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    api(libs.mybatisFlexStarter)
    implementation(libs.mybatisFlexCodegen)
    annotationProcessor(libs.mybatisFlexProcessor)
    implementation(libs.mybatisFlexReactorSpring)
    implementation(libs.hikariCp)
    constraints {
        implementation(libs.commonsCompress) {
            because("fix CVE-2024-25710 and CVE-2024-26308")
        }
        implementation(libs.poi) {
            because("fix CVE-2025-31672")
        }
        implementation(libs.poiOoxml)
        implementation(libs.jacksonCore) {
            because("fix CVE-2024-50379 - Jackson async parser DoS")
        }
        implementation(libs.jacksonCore) {
            because("fix CVE-2024-50379 - Jackson async parser DoS")
        }
    }

    api(libs.jjwtApi)
    runtimeOnly(libs.jjwtImpl)
    runtimeOnly(libs.jjwtJackson)

    api(libs.springdocScalarWebmvc)

    implementation(libs.ip2region)
    implementation(libs.yauaa)
    implementation(libs.oshiCore)
    api(libs.minio)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "flexboot4-admin-starter"
        }
    }
}

