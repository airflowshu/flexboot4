package {{packageBase}}.controller.{{domain}};

import {{packageBase}}.config.ApiTagGroup;
import {{packageBase}}.controller.sys.EntityCrudController;
import {{packageBase}}.dto.{{domain}}.{{entity}}CreateReq;
import {{packageBase}}.dto.{{domain}}.{{entity}}UpdateReq;
import {{packageBase}}.entity.{{domain}}.{{entity}};
import {{packageBase}}.service.{{domain}}.{{entity}}Service;
import {{packageBase}}.vo.{{domain}}.{{entity}}DetailVO;
import {{packageBase}}.vo.{{domain}}.{{entity}}ListVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("{{basePath}}")
@Tag(name = "{{entity}}管理", description = "{{entity}} - {{resource}} management")
@ApiTagGroup(group = "{{apiGroup}}")
public class {{entity}}Controller extends EntityCrudController<{{entity}}Service, {{entity}}, {{idType}},
        {{entity}}CreateReq, {{entity}}UpdateReq, {{entity}}ListVO, {{entity}}DetailVO> {

    public {{entity}}Controller({{entity}}Service service) {
        super(service, {{entity}}.class, {{entity}}ListVO.class, {{entity}}DetailVO.class);
    }

    @Override
    public Class<{{entity}}> getEntityClass() {
        return {{entity}}.class;
    }
}
