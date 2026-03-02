package ${packageConfig.mapperPackage};

import org.apache.ibatis.annotations.Mapper;
import com.mybatisflex.core.BaseMapper;
import ${packageConfig.entityPackage}.${table.buildEntityClassName()};

@Mapper
public interface ${table.buildEntityClassName()}Mapper extends BaseMapper<${table.buildEntityClassName()}> {
}

