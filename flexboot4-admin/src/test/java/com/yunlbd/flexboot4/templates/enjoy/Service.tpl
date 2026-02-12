package ${packageConfig.servicePackage};

import com.mybatisflex.core.service.IService;
import ${packageConfig.entityPackage}.${table.buildEntityClassName()};

public interface ${table.buildEntityClassName()}Service extends IService<${table.buildEntityClassName()}> {
}

