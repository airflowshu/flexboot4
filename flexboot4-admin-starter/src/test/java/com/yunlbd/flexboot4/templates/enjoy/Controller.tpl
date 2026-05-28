package ${packageConfig.controllerPackage};

import com.yunlbd.flexboot4.controller.sys.EntityCrudController;
import ${packageConfig.dtoPackage}.${table.buildEntityClassName()}CreateReq;
import ${packageConfig.dtoPackage}.${table.buildEntityClassName()}UpdateReq;
import ${packageConfig.entityPackage}.${table.buildEntityClassName()};
import ${packageConfig.servicePackage}.${table.buildEntityClassName()}Service;
import ${packageConfig.voPackage}.${table.buildEntityClassName()}DetailVO;
import ${packageConfig.voPackage}.${table.buildEntityClassName()}ListVO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/${table.name?lower_case?replace('_','-')}")
public class ${table.buildEntityClassName()}Controller extends EntityCrudController<${table.buildEntityClassName()}Service, ${table.buildEntityClassName()}, String,
        ${table.buildEntityClassName()}CreateReq, ${table.buildEntityClassName()}UpdateReq, ${table.buildEntityClassName()}ListVO, ${table.buildEntityClassName()}DetailVO> {

    public ${table.buildEntityClassName()}Controller(${table.buildEntityClassName()}Service service) {
        super(service, ${table.buildEntityClassName()}.class, ${table.buildEntityClassName()}ListVO.class, ${table.buildEntityClassName()}DetailVO.class);
    }

    @Override
    public Class<${table.buildEntityClassName()}> getEntityClass() {
        return ${table.buildEntityClassName()}.class;
    }
}
