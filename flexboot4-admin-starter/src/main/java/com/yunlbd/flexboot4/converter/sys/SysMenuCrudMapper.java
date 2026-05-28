package com.yunlbd.flexboot4.converter.sys;

import com.yunlbd.flexboot4.converter.BaseMapStructConfig;
import com.yunlbd.flexboot4.converter.CrudMapper;
import com.yunlbd.flexboot4.dto.sys.SysMenuCreateReq;
import com.yunlbd.flexboot4.dto.sys.SysMenuUpdateReq;
import com.yunlbd.flexboot4.entity.sys.SysMenu;
import com.yunlbd.flexboot4.excel.sys.SysMenuExportRow;
import com.yunlbd.flexboot4.vo.sys.SysMenuDetailVO;
import com.yunlbd.flexboot4.vo.sys.SysMenuListVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapStructConfig.class)
public interface SysMenuCrudMapper extends CrudMapper<SysMenu, SysMenuCreateReq, SysMenuUpdateReq, SysMenuListVO, SysMenuDetailVO> {

    @Override
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "roles", ignore = true)
    SysMenu toEntity(SysMenuCreateReq request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "lastModifyTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "lastModifyBy", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "roles", ignore = true)
    void updateEntity(SysMenuUpdateReq request, @MappingTarget SysMenu entity);

    SysMenuExportRow toExportRow(SysMenu entity);
}
