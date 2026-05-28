plugins {
    alias(libs.plugins.springDependencyManagement)
    `java-library`
    `maven-publish`
}

description = "flexboot4 - Admin Kernel (Shared Base Layer)"

dependencies {
    api(project(":flexboot4-core"))

    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-cache")
    api("org.springframework.boot:spring-boot-starter-validation")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    api(libs.mapstruct)
    annotationProcessor(libs.mapstructProcessor)
    api(libs.mybatisFlexStarter)
    api(libs.postgresqlJdbc)
    implementation(libs.mybatisFlexCodegen)
    annotationProcessor(libs.mybatisFlexProcessor)
    implementation(libs.mybatisFlexReactorSpring)

    api(libs.easyexcel)
    api(libs.springdocScalarWebmvc)
    api("io.projectreactor:reactor-core")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "flexboot4-admin-kernel"
        }
    }
}
