package {{packageBase}}.controller.{{domain}};

import {{packageBase}}.config.ApiTagGroup;
import {{packageBase}}.controller.sys.BaseCrudController;
import {{packageBase}}.controller.sys.CrudExcelSupport;
import {{packageBase}}.controller.sys.CrudFieldPolicy;
import {{packageBase}}.converter.{{domain}}.{{entity}}CrudMapper;
import {{packageBase}}.dto.{{domain}}.{{entity}}CreateReq;
import {{packageBase}}.dto.{{domain}}.{{entity}}UpdateReq;
import {{packageBase}}.entity.{{domain}}.{{entity}};
import {{packageBase}}.excel.{{domain}}.{{entity}}ExportRow;
import {{packageBase}}.excel.{{domain}}.{{entity}}ImportRow;
import {{packageBase}}.service.{{domain}}.{{entity}}Service;
import {{packageBase}}.vo.{{domain}}.{{entity}}DetailVO;
import {{packageBase}}.vo.{{domain}}.{{entity}}ListVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("{{basePath}}")
@Tag(name = "{{entity}}管理", description = "{{entity}} - {{resource}} management")
@ApiTagGroup(group = "{{apiGroup}}")
public class {{entity}}Controller extends BaseCrudController<{{entity}}Service, {{entity}}, {{idType}},
        {{entity}}CreateReq, {{entity}}UpdateReq, {{entity}}ListVO, {{entity}}DetailVO> {

    private final {{entity}}CrudMapper mapper;

    public {{entity}}Controller({{entity}}Service service, {{entity}}CrudMapper mapper) {
        super(service, mapper);
        this.mapper = mapper;
    }

    @Override
    public Class<{{entity}}> getEntityClass() {
        return {{entity}}.class;
    }

    @Override
    protected CrudFieldPolicy fieldPolicy() {
        return CrudFieldPolicy.of(
                List.of({{queryFields}}),
                List.of({{orderFields}})
        ){{relationQueryFields}};
    }

    @Override
    protected CrudExcelSupport<{{entity}}, ?, ?> excelSupport() {
        return CrudExcelSupport.of({{entity}}ExportRow.class, {{entity}}ImportRow.class, mapper::toExportRow, null);
    }
}
