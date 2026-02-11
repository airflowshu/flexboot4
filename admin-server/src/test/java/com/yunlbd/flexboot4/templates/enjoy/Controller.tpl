package ${packageConfig.controllerPackage};

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yunlbd.flexboot4.controller.BaseController;
import ${packageConfig.servicePackage}.${table.buildEntityClassName()}Service;
import ${packageConfig.entityPackage}.${table.buildEntityClassName()};

@RestController
@RequestMapping("/api/${table.name?lower_case?replace('_','-')}")
public class ${table.buildEntityClassName()}Controller extends BaseController<${table.buildEntityClassName()}Service, ${table.buildEntityClassName()}, String> {
    @Override
    protected Class<${table.buildEntityClassName()}> getEntityClass() {
        return ${table.buildEntityClassName()}.class;
    }
}

