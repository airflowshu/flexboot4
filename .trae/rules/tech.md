项目使用的主要技术栈包括：
- 后端：Springboot4、Java25、Spring Security、JWT、MyBatis-Flex（v1.11.5）、gradle、kotlin、Spring Cache
- 数据库：PostgreSQL/MySQL、Redis

项目采用Restful风格的API接口，集成OPENAPI3，API接口格式能适配开源项目：https://github.com/vbenjs/vue-vben-admin

在项目代码可能出现重构实现时，无需考虑与前端对接的兼容性问题，因为目前还未实现具体业务功能对接。

项目运行环境目前是在Java 25，不要修改build.gradle.kts 中配置的 languageVersion = JavaLanguageVersion.of(25)。

