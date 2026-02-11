# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :admin-server:build

# Run application (admin-server)
./gradlew :admin-server:bootRun

# Run tests
./gradlew test

# Run specific module tests
./gradlew :admin-server:test

# Run single test class
./gradlew :admin-server:test --tests "com.yunlbd.flexboot4.Flexboot4ApplicationTests"

# Clean build
./gradlew clean

# Clean and rebuild
./gradlew clean build
```

## Project Structure

This is a multi-module Gradle project with the following modules:

### Core Modules
- **admin-server**: Main admin backend application (Spring Boot 4)
- **ai-gateway**: AI gateway service
- **common**: Shared utilities and common code

### Module-Specific Commands

```bash
# Admin Server (Main API Server)
./gradlew :admin-server:bootRun

# AI Gateway
./gradlew :ai-gateway:bootRun

# Build common module
./gradlew :common:build
```

## Project Overview

Spring Boot 4 REST API project using MyBatis-Flex for data access. Features include:
- JWT-based authentication with Spring Security
- Dynamic query builder supporting single-table and multi-table join queries
- Redis-based caching with version-controlled cache invalidation
- Excel export with reactive streaming
- OpenAPI 3 documentation (accessible at /v3/api-docs and /scalar)
- **Dynamic table-based operation logging system** (Quarterly partitioning)
- **User-Agent parsing and terminal information collection**
- **Method execution time tracking** (cost_time)

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

### Dynamic Table-Based Operation Logging

**New Feature**: Quarterly partitioning for operation logs to improve performance and manageability.

- **Table naming**: `sys_oper_log_YYYY_qN` (e.g., `sys_oper_log_2026_q1`)
- **Dynamic query builder**: Automatically builds UNION ALL queries across multiple quarterly tables
- **Controller**: `SysOperLogController` at `/api/oper-log/page`
- **Utilities**: `LogTableUtils.java` for table name generation

#### Key Components
```java
// SysOperLog entity with enhanced fields
public class SysOperLog extends BaseEntity {
    private String terminal;           // User-Agent parsed info
    private Long costTime;              // Method execution time (milliseconds)
    private Map<String, Object> operParam;   // Request parameters
    private Map<String, Object> jsonResult;  // Response data
}

// Dynamic table processing
@TableManager.setDynamicTableProcessor(tableName -> {
    if ("sys_oper_log".equals(tableName)) {
        // Auto-append quarterly suffix
        return tableName + "_" + year + "_q" + quarter;
    }
    return tableName;
});
```

### Cache Invalidation
Table versions tracked in Redis. Write operations auto-bump version; reads use versioned cache keys. Configure via `DynamicCacheResolver`.

### Dictionary Handling
Use `@DictEnum("dictTypeCode")` on entity fields. `GlobalDictSetListener` automatically populates `<fieldName>Str` fields on read.

### User-Agent Parsing
**New Feature**: Automatic terminal information collection using `yauaa` library.

```java
// User-Agent parsing service
@Service
public class UserAgentService {
    public Map<String, String> parseRequest(HttpServletRequest request);
}

// LogAspect automatically collects terminal info
@Around("@annotation(controllerLog)")
public Object doAround(ProceedingJoinPoint joinPoint, OperLog controllerLog) {
    // Start time recorded
    // After execution, terminal info extracted and saved
    String userAgent = request.getHeader("User-Agent");
    Map<String, String> terminalInfo = userAgentService.parseRequest(request);
    // Stores: system, browser, device, version
}
```

### Method Execution Time Tracking
**New Feature**: Automatic cost_time calculation for all methods annotated with `@OperLog`.

```java
// LogAspect uses ScopedValue to track execution time
@Around("@annotation(controllerLog)")
public Object doAround(ProceedingJoinPoint joinPoint, OperLog controllerLog) {
    START_TIME.set(LocalDateTime.now());
    try {
        return joinPoint.proceed();
    } finally {
        // cost_time calculated and saved to SysOperLog
    }
}
```

## Key Files

### Core Architecture
- `query/DefaultQueryWrapperBuilder.java` - Universal query builder with multi-table support
- `cache/TableVersions.java` - Version-based cache invalidation
- `listener/GlobalDictSetListener.java` - Dictionary text resolution
- `entity/BaseEntity.java` - Common entity fields (id, version, delFlag, createTime, etc.)
- `security/JwtAuthenticationFilter.java` - JWT token processing

### New Features (2026)
- `controller/ops/SysOperLogController.java` - Dynamic table-based operation log queries
- `util/LogTableUtils.java` - Quarterly table name generation
- `util/UserAgentService.java` - User-Agent parsing with yauaa library
- `common/aspect/LogAspect.java` - AOP logging with cost_time tracking
- `event/SysOperLogEvent.java` - Operation log event publishing
- `listener/SysOperLogListener.java` - Async operation log persistence

## API Documentation

- **OpenAPI 3**: `/v3/api-docs` - JSON format
- **Scalar UI**: `/scalar` - Interactive API documentation

## Tech Stack

- **Java 25** (latest version, configured in build.gradle.kts)
- Spring Boot 4.0.1
- Spring Security + JWT
- MyBatis-Flex 1.11.5
- PostgreSQL + Redis
- Gradle (Kotlin DSL)
- **YAUAA**: User-Agent parsing library
- **EasyExcel**: Excel export functionality

## Database Design

### Operation Logging Tables
- `sys_oper_log_YYYY_qN` - Quarterly partitioned operation logs
- Fields: id, title, business_type, oper_name, oper_ip, terminal, cost_time, oper_time, etc.
- Supports: UNION ALL queries across multiple quarterly tables

### Common Tables
- `sys_user` - User management
- `sys_role` - Role management
- `sys_menu` - Menu permissions
- `sys_dept` - Department hierarchy
- `sys_dict_type` / `sys_dict_item` - Dictionary data

## Development Guidelines

### Adding New Features
1. Extend `BaseController` for CRUD operations
2. Use `@OperLog` annotation for automatic operation logging
3. Implement `@CacheConfig` for service-level caching
4. Use `SearchDto` for universal query support

### Best Practices
1. Use `@DictEnum` for dictionary fields
2. Leverage `DefaultQueryWrapperBuilder` for complex queries
3. Follow the quarterly partitioning pattern for large datasets
4. Utilize `UserAgentService` for terminal information collection