package com.yunlbd.flexboot4.converter.ops;

import com.yunlbd.flexboot4.converter.BaseMapStructConfig;
import com.yunlbd.flexboot4.converter.CrudMapper;
import com.yunlbd.flexboot4.dto.ops.SysConfigCreateReq;
import com.yunlbd.flexboot4.dto.ops.SysConfigUpdateReq;
import com.yunlbd.flexboot4.entity.ops.SysConfig;
import com.yunlbd.flexboot4.excel.ops.SysConfigExportRow;
import com.yunlbd.flexboot4.vo.ops.SysConfigDetailVO;
import com.yunlbd.flexboot4.vo.ops.SysConfigListVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapStructConfig.class)
public interface SysConfigCrudMapper extends CrudMapper<SysConfig, SysConfigCreateReq, SysConfigUpdateReq, SysConfigListVO, SysConfigDetailVO> {

    @Override
    SysConfig toEntity(SysConfigCreateReq request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "lastModifyTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "lastModifyBy", ignore = true)
    @Mapping(target = "statusStr", ignore = true)
    void updateEntity(SysConfigUpdateReq request, @MappingTarget SysConfig entity);

    SysConfigExportRow toExportRow(SysConfig entity);
}
