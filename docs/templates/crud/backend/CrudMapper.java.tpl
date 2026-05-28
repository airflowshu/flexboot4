package {{packageBase}}.converter.{{domain}};

import {{packageBase}}.converter.BaseMapStructConfig;
import {{packageBase}}.converter.CrudMapper;
import {{packageBase}}.dto.{{domain}}.{{entity}}CreateReq;
import {{packageBase}}.dto.{{domain}}.{{entity}}UpdateReq;
import {{packageBase}}.entity.{{domain}}.{{entity}};
import {{packageBase}}.excel.{{domain}}.{{entity}}ExportRow;
import {{packageBase}}.vo.{{domain}}.{{entity}}DetailVO;
import {{packageBase}}.vo.{{domain}}.{{entity}}ListVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapStructConfig.class)
public interface {{entity}}CrudMapper extends CrudMapper<{{entity}}, {{entity}}CreateReq, {{entity}}UpdateReq,
        {{entity}}ListVO, {{entity}}DetailVO> {

    @Override
    {{createMappings}}
    {{entity}} toEntity({{entity}}CreateReq request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "lastModifyTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "lastModifyBy", ignore = true)
    {{updateMappings}}
    void updateEntity({{entity}}UpdateReq request, @MappingTarget {{entity}} entity);

    {{entity}}ExportRow toExportRow({{entity}} entity);
}
