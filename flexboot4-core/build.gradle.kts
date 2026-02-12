plugins {
    `java-library`
}

description = "flexboot4 - Common pure shared library"

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
