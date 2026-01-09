plugins {
	java
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.yunlbd"
version = "0.0.1-SNAPSHOT"
description = "study project for Spring Boot4"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-validation")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// Database
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	runtimeOnly("org.postgresql:postgresql")

	// MyBatis Flex
	implementation("com.mybatis-flex:mybatis-flex-spring-boot4-starter:1.11.5")
	implementation("com.mybatis-flex:mybatis-flex-codegen:1.11.5")
	annotationProcessor("com.mybatis-flex:mybatis-flex-processor:1.11.5")
    implementation("com.juxest:mybatis-flex-reactor-spring:0.2.2")
    //添加com.zaxxer.HikariCP的数据源
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("com.alibaba:easyexcel:3.1.1")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.13.0")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

	// OpenAPI
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-scalar:3.0.0")

    // Test
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
    options.encoding = "UTF-8"
}