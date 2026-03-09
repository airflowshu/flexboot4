plugins {
    `java-platform`
    `maven-publish`
}

description = "flexboot4 - BOM (Bill of Materials)"

dependencies {
    constraints {
        api(project(":flexboot4-core"))
        api(project(":flexboot4-admin-starter"))
        api(project(":flexboot4-kb-starter"))
        api(project(":flexboot4-media-starter"))
        api(project(":flexboot4-cms-starter"))
    }
}

publishing {
    publications {
        create<MavenPublication>("bom") {
            from(components["javaPlatform"])
            artifactId = "flexboot4-bom"
        }
    }
}
