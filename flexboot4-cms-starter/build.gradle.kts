plugins {
    alias(libs.plugins.springDependencyManagement)
    `java-library`
    `maven-publish`
}

description = "flexboot4 - CMS Starter"

dependencies {
    api(project(":flexboot4-admin-starter"))

    implementation(libs.mybatisFlexStarter)
    annotationProcessor(libs.mybatisFlexProcessor)

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "flexboot4-cms-starter"
        }
    }
}

