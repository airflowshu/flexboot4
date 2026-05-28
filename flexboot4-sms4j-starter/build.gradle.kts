plugins {
    alias(libs.plugins.springDependencyManagement)
    `java-library`
    `maven-publish`
}

description = "flexboot4 - SMS4J Starter (Multi-channel SMS Integration)"

dependencies {
    api(project(":flexboot4-admin-kernel"))

    // sms4j BOM + spring-boot starter
    implementation(platform(libs.sms4jBom))
    implementation(libs.sms4jSpringBoot)
    implementation(libs.sms4jCore)

    implementation(libs.mybatisFlexStarter)
    annotationProcessor(libs.mybatisFlexProcessor)

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor(libs.mapstructProcessor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "flexboot4-sms4j-starter"
        }
    }
}

