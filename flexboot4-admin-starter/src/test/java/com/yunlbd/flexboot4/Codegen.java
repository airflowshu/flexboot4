package com.yunlbd.flexboot4;

import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.mybatisflex.codegen.template.impl.EnjoyTemplate;
import com.mybatisflex.core.BaseMapper;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import com.zaxxer.hikari.HikariDataSource;

public class Codegen {

    static void main(String[] args) {
        //配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://192.168.11.104:5433/flexboot4");
        dataSource.setUsername("flexboot4");
        dataSource.setPassword("flexboot4");

        //创建配置内容，两种风格都可以。
        GlobalConfig globalConfig = createGlobalConfigUseStyle1();
        // GlobalConfig globalConfig = createGlobalConfigUseFlexBoot4Style();

        //通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);

        //生成代码
        generator.generate();
    }

    public static GlobalConfig createGlobalConfigUseStyle1() {
        //创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.getJavadocConfig().setAuthor("Wangts").setSince("1.0.0");

        //设置根包
        globalConfig.getPackageConfig()
                .setSourceDir("E:\\vsWorkspace\\flexboot4\\admin-server\\src\\main\\java")
                .setBasePackage("com.yunlbd.flexboot4");

        //设置表前缀和只生成哪些表
        // globalConfig.setTablePrefix("sys_");
        globalConfig.setGenerateTable("ai_api_key");
        // globalConfig.setGenerateTable("sys_dict_item", "sys_dict_type");

        //设置项目的JDK版本，项目的JDK为14及以上时建议设置该项，小于14则可以不设置
        globalConfig.setEntityJdkVersion(25);

        //设置生成 entity 并启用 Lombok
        globalConfig.setEntityGenerateEnable(true);
        globalConfig.setEntityWithLombok(true);
        globalConfig.setEntitySuperClass(BaseEntity.class);
        //设置生成 mapper
        globalConfig.enableMapper()
                .setSuperClass(BaseMapper.class)
                .setMapperAnnotation(true);
        //feat 想生成那个类，就指定true....
        globalConfig.enableController();
        globalConfig.enableService();
        globalConfig.enableServiceImpl();

        //可以单独配置某个列
        // ColumnConfig columnConfig = new ColumnConfig();
        // columnConfig.setColumnName("tenant_id");
        // columnConfig.setLarge(true);
        // columnConfig.setVersion(true);
        // globalConfig.setColumnConfig("tb_account", columnConfig);

        return globalConfig;
    }

    public static GlobalConfig createGlobalConfigUseFlexBoot4Style() {
        //创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.getJavadocConfig().setAuthor("Wangts").setSince("1.0.0");

        globalConfig.getPackageConfig()
                .setSourceDir("E:\\vsWorkspace\\flexboot4\\admin-server\\src\\main\\java")
                .setBasePackage("com.yunlbd.flexboot4");

        globalConfig.getStrategyConfig()
                // .setTablePrefix("sys_")
                .setGenerateTable("sys_role");

        globalConfig.setEntityJdkVersion(25);

        globalConfig.enableEntity()
                .setWithLombok(true)
                .setSuperClass(BaseEntity.class);
        globalConfig.enableMapper()
                .setSuperClass(BaseMapper.class)
                .setMapperAnnotation(true);
        globalConfig.enableController();
        globalConfig.enableService();
        globalConfig.enableServiceImpl();

        globalConfig.getTemplateConfig()
                .setTemplate(new EnjoyTemplate())
                .setEntity("e:/vsWorkspace/flexboot4/src/test/java/com/yunlbd/flexboot4/templates/enjoy/Entity.tpl")
                .setMapper("e:/vsWorkspace/flexboot4/src/test/java/com/yunlbd/flexboot4/templates/enjoy/Mapper.tpl")
                .setService("e:/vsWorkspace/flexboot4/src/test/java/com/yunlbd/flexboot4/templates/enjoy/Service.tpl")
                .setServiceImpl("e:/vsWorkspace/flexboot4/src/test/java/com/yunlbd/flexboot4/templates/enjoy/ServiceImpl.tpl")
                .setController("e:/vsWorkspace/flexboot4/src/test/java/com/yunlbd/flexboot4/templates/enjoy/Controller.tpl");

        return globalConfig;
    }
}
