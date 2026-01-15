# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build project
./gradlew build

# Run application
./gradlew bootRun

# Run tests
./gradlew test

# Run single test class
./gradlew test --tests "com.yunlbd.flexboot4.Flexboot4ApplicationTests"

# Clean build
./gradlew clean
```

## Project Overview

Spring Boot 4 REST API project using MyBatis-Flex for data access. Features include:
- JWT-based authentication with Spring Security
- Dynamic query builder supporting single-table and multi-table join queries
- Redis-based caching with version-controlled cache invalidation
- Excel export with reactive streaming
- OpenAPI 3 documentation (accessible at /v3/api-docs and /scalar)

## Architecture Patterns

### Layer Structure
- **Controller**: Extends `BaseController<S, T, ID>` for CRUD operations; override `getEntityClass()` in subclass
- **Service**: Extends `BaseServiceImpl<M, T>` which implements `IExtendedService<T>`; use `@CacheConfig(cacheNames = "...")` on subclasses
- **Mapper**: MyBatis-Flex base mappers extending `BaseMapper<T>`
- **Entity**: Extends `BaseEntity` with auto-generated String ID (snowflake)

### Entity Relations
```java
@RelationManyToOne(selfField = "deptId", targetField = "id")
private SysDept dept;

@RelationManyToMany(
    joinTable = "sys_user_role",
    selfField = "id", joinSelfColumn = "user_id",
    targetField = "id", joinTargetColumn = "role_id"
)
private List<SysRole> roles;
```

### Universal Query API
All controllers support `/page` and `/list` endpoints accepting `SearchDto`:
- **Single table**: `{ "field": "status", "op": "eq", "val": 1 }`
- **Join query**: `{ "field": "dept.deptName", "op": "like", "val": "研发" }`
- **Nested conditions**: Use `children` with `logic` (AND/OR)
- **Ordering**: `{ "column": "createTime", "asc": false }`

### Cache Invalidation
Table versions tracked in Redis. Write operations auto-bump version; reads use versioned cache keys. Configure via `DynamicCacheResolver`.

### Dictionary Handling
Use `@DictEnum("dictTypeCode")` on entity fields. `GlobalDictSetListener` automatically populates `<fieldName>Str` fields on read.

## Key Files

- `query/DefaultQueryWrapperBuilder.java` - Universal query builder
- `cache/TableVersions.java` - Version-based cache invalidation
- `listener/GlobalDictSetListener.java` - Dictionary text resolution
- `entity/BaseEntity.java` - Common entity fields (id, version, delFlag, createTime, etc.)
- `security/JwtAuthenticationFilter.java` - JWT token processing

## Tech Stack

- Java 21+ (configured in build.gradle.kts)
- Spring Boot 4.0.1
- Spring Security + JWT
- MyBatis-Flex 1.11.5
- PostgreSQL + Redis
- Gradle (Kotlin DSL)