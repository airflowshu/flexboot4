package ${packageConfig.serviceImplPackage};

import org.springframework.stereotype.Service;
import com.yunlbd.flexboot4.service.impl.BaseServiceImpl;
import ${packageConfig.mapperPackage}.${table.buildEntityClassName()}Mapper;
import ${packageConfig.servicePackage}.${table.buildEntityClassName()}Service;
import ${packageConfig.entityPackage}.${table.buildEntityClassName()};

@Service
public class ${table.buildEntityClassName()}ServiceImpl extends BaseServiceImpl<${table.buildEntityClassName()}Mapper, ${table.buildEntityClassName()}> implements ${table.buildEntityClassName()}Service {
}

