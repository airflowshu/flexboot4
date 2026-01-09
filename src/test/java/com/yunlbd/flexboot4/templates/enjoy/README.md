模板说明

- 目录：src/test/java/com/yunlbd/flexboot4/templates/enjoy
- 引擎：FreeMarker
- 文件：Entity.tpl、Mapper.tpl、Service.tpl、ServiceImpl.tpl、Controller.tpl

使用步骤

1. 在 Codegen.createGlobalConfigUseFlexBoot4Style 中已配置模板路径与包名
2. 提供数据库连接并运行生成器：
   - 运行 Codegen.main 或在测试中设置 FLEXBOOT4_DB_URL、FLEXBOOT4_DB_USER、FLEXBOOT4_DB_PASS 环境变量
3. 生成位置：src/main/java/com/yunlbd/flexboot4 下的各层包

注意事项

- 避免与 APT 的 Mapper 生成冲突
- 保持 JDK 版本为 25
