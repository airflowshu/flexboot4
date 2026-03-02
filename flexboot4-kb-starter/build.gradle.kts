plugins {
    alias(libs.plugins.springDependencyManagement)
    `java-library`
    `maven-publish`
}

description = "flexboot4 - Knowledge Base Starter"


dependencies {
    api(project(":flexboot4-admin-starter"))

    implementation(libs.pdfbox)
    implementation(libs.poiOoxml)

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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "flexboot4-kb-starter"
        }
    }
}

