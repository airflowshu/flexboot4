package com.yunlbd.flexboot4.converter.sys;

import com.yunlbd.flexboot4.converter.BaseMapStructConfig;
import com.yunlbd.flexboot4.converter.CrudMapper;
import com.yunlbd.flexboot4.dto.sys.SysUserCreateReq;
import com.yunlbd.flexboot4.dto.sys.SysUserUpdateReq;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.excel.sys.SysUserExportRow;
import com.yunlbd.flexboot4.vo.sys.SysUserDetailVO;
import com.yunlbd.flexboot4.vo.sys.SysUserListVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapStructConfig.class)
public interface SysUserCrudMapper extends CrudMapper<SysUser, SysUserCreateReq, SysUserUpdateReq, SysUserListVO, SysUserDetailVO> {

    @Override
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "dept", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "profileFile", ignore = true)
    SysUser toEntity(SysUserCreateReq request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "lastModifyTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "lastModifyBy", ignore = true)
    @Mapping(target = "dept", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "profileFile", ignore = true)
    void updateEntity(SysUserUpdateReq request, @MappingTarget SysUser entity);

    SysUserExportRow toExportRow(SysUser entity);
}
