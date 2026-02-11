plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    java
}

description = "flexboot4 - AI Gateway (Springboot4 WebFlux)"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework:spring-aop")
    implementation("org.aspectj:aspectjweaver")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    implementation(libs.jjwtApi)
    runtimeOnly(libs.jjwtImpl)
    runtimeOnly(libs.jjwtJackson)

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation(libs.r2dbcPostgresql)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")

    implementation(project(":common"))
}
