plugins {
    alias(libs.plugins.springDependencyManagement)
    `java-library`
    `maven-publish`
}

description = "flexboot4 - Media Starter"


dependencies {
    api(project(":flexboot4-admin-kernel"))

    implementation(libs.mybatisFlexStarter)
    annotationProcessor(libs.mybatisFlexProcessor)

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor(libs.mapstructProcessor)

    implementation(libs.jainSipApi)
    implementation(libs.jainSipRi)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "flexboot4-media-starter"
        }
    }
}
