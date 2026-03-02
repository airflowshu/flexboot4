# FlexBoot4 Agent Guidelines

This document provides essential information for AI agents working in the FlexBoot4 repository. It outlines the build system, architecture, and coding standards required to maintain consistency across the project.

## 🛠 Build and Test Commands

FlexBoot4 is a multi-module Gradle project (Java 25). Always run commands from the project root using the Gradle wrapper.

### Project-wide Commands
- **Build all modules**: `./gradlew build`
- **Clean and rebuild**: `./gradlew clean build`
- **Run all tests**: `./gradlew test`
- **Check dependencies**: `./gradlew dependencies`

### Module-specific Commands
- **Admin Server**: `./gradlew :flexboot4-admin:bootRun`
- **AI Gateway**: `./gradlew :flexboot4-ai:bootRun`
- **Knowledge Base**: `./gradlew :flexboot4-kb:build`
- **Bootstrap (All-in-one)**: `./gradlew :flexboot4-bootstrap:bootRun`

### Targeted Testing
- **Single module tests**: `./gradlew :flexboot4-admin:test`
- **Single test class**: `./gradlew :flexboot4-admin:test --tests "com.yunlbd.flexboot4.Flexboot4ApplicationTests"`
- **Single test method**: `./gradlew :flexboot4-admin:test --tests "com.yunlbd.flexboot4.util.IpUtilsTest.testIpV4"`

## 🏗 Architecture & Module structure

- **flexboot4-core**: Foundation module. Contains `BaseEntity`, `ApiResult`, `SearchDto`, and shared utilities. No Spring Boot dependencies if possible.
- **flexboot4-admin**: Management backend (Spring MVC). Handles RBAC, system config, and audit logs.
- **flexboot4-ai**: AI High-performance gateway (Spring WebFlux). Uses R2DBC and reactive patterns.
- **flexboot4-kb**: Knowledge Base extension. Handles file parsing (PDF/Word) and vectorization pipelines.
- **flexboot4-bootstrap**: Assembly module used for starting the integrated application.

### Layering Conventions
- **Controller**: Extends `BaseController<S, T, String>`. Override `getEntityClass()`.
- **Service**: 
    - Interface `${Entity}Service` extends `IExtendedService<T>`.
    - Implementation `${Entity}ServiceImpl` extends `BaseServiceImpl<M, T>` and implements the service interface.
- **Mapper**: Interface `${Entity}Mapper` extends `BaseMapper<T>` with `@Mapper`.
- **Entity**: Extends `BaseEntity`. Use MyBatis-Flex annotations.
- **DTO**: Use records for simple data carriers where possible, or Lombok-annotated classes for complex objects.

## 🎨 Code Style Guidelines

### Language & Tooling
- **Java 25**: Use modern features like `record`, `sealed classes`, `pattern matching`, and `ScopedValue`.
- **Lombok**: Essential. Use `@Data`, `@SuperBuilder`, `@RequiredArgsConstructor`. Avoid manual getter/setter/constructor creation.
- **Constructors**: Prefer constructor-based dependency injection via `@RequiredArgsConstructor`. NEVER use `@Autowired` on fields.

### Naming Conventions
- **Classes**: PascalCase. Impl classes must end in `Impl`.
- **Methods**: camelCase. Use descriptive names like `findActiveUsersByDeptId`.
- **Variables**: camelCase. Avoid single-letter variables except in lambdas.
- **Tables**: snake_case (e.g., `sys_user_role`).
- **Columns**: snake_case (e.g., `create_time`).

### Imports
1. `java.*` / `javax.*` / `jakarta.*`
2. Third-party libraries (`org.*`, `com.*`, etc.)
3. Internal project packages (`com.yunlbd.flexboot4.*`)

Avoid wildcard imports (`import java.util.*`) unless more than 10 classes from the same package are used.

## 💾 Persistence (MyBatis-Flex)

### Entity Mapping
Use `@Table`, `@Column`, and `@Id`. For relations, use:
- `@RelationManyToOne`: For foreign key relations.
- `@RelationManyToMany`: For join table relations.
- `@RelationOneToMany`: For collections.

### Type-Safe Queries
Always use the generated `TableDef` classes (e.g., `USER_TABLE`) for building queries.
```java
QueryWrapper query = QueryWrapper.create()
    .select(USER.ALL_COLUMNS)
    .where(USER.ID.eq(id));
```

### Universal Search
The project uses `SearchDto` for dynamic filtering. Use `DefaultQueryWrapperBuilder` to convert these DTOs into MyBatis-Flex wrappers.

## 🛡 Security & Error Handling

### Security
- Use `SecurityUtils` to get current user info.
- Authenticated via JWT. Tokens are processed in `JwtAuthenticationFilter`.
- Permission checks via `@RequirePermission`.

### Exceptions
- Throw specialized exceptions where appropriate.
- Handled globally by `GlobalExceptionHandler`.
- Response format is ALWAYS `ApiResult<T>`.

## 🤖 AI & Reactive Programming (WebFlux)
In `flexboot4-ai`, strictly follow reactive principles:
- No blocking calls (e.g., `Thread.sleep`, `InputStream.read`).
- Use `WebClient` for external calls.
- Prefer `Flux` and `Mono` over `List` or direct objects.
- Use `r2dbc-postgresql` for database operations.

## 📝 Logging & Auditing
- **AOP Logging**: Use `@OperLog` on controller methods.
- **Table Partitioning**: Operation logs are partitioned by quarter (`sys_oper_log_2026_q1`). Use `LogTableUtils` for dynamic table resolution.
- **Terminal Info**: `UserAgentService` automatically parses terminal info for logging.

## ⚙️ Versions & Dependencies
- All versions are managed in `gradle/libs.versions.toml`.
- Do not hardcode versions in sub-module `build.gradle.kts`.
- Use `libs.[dependencyName]` reference.
