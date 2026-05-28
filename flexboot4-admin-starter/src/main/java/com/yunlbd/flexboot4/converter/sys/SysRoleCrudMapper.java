package com.yunlbd.flexboot4.converter.sys;

import com.yunlbd.flexboot4.converter.BaseMapStructConfig;
import com.yunlbd.flexboot4.converter.CrudMapper;
import com.yunlbd.flexboot4.dto.sys.SysRoleCreateReq;
import com.yunlbd.flexboot4.dto.sys.SysRoleUpdateReq;
import com.yunlbd.flexboot4.entity.sys.SysRole;
import com.yunlbd.flexboot4.excel.sys.SysRoleExportRow;
import com.yunlbd.flexboot4.vo.sys.SysRoleDetailVO;
import com.yunlbd.flexboot4.vo.sys.SysRoleListVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapStructConfig.class)
public interface SysRoleCrudMapper extends CrudMapper<SysRole, SysRoleCreateReq, SysRoleUpdateReq, SysRoleListVO, SysRoleDetailVO> {

    @Override
    @Mapping(target = "menus", ignore = true)
    @Mapping(target = "users", ignore = true)
    SysRole toEntity(SysRoleCreateReq request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "lastModifyTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "lastModifyBy", ignore = true)
    @Mapping(target = "menus", ignore = true)
    @Mapping(target = "users", ignore = true)
    void updateEntity(SysRoleUpdateReq request, @MappingTarget SysRole entity);

    SysRoleExportRow toExportRow(SysRole entity);
}
