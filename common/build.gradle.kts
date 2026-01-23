plugins {
    `java-library`
}

description = "Common pure shared library"

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
