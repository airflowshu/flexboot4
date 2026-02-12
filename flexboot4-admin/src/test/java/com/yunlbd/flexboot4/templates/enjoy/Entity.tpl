package ${packageConfig.entityPackage};

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yunlbd.flexboot4.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("${table.name}")
public class ${table.buildEntityClassName()} extends BaseEntity {
<#list table.columns as col>
    private ${col.javaType} ${col.property};
</#list>
}

