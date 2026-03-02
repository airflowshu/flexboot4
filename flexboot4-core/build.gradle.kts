plugins {
    `java-library`
    `maven-publish`
}

description = "flexboot4 - Common pure shared library"

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "flexboot4-core"
        }
    }
}

