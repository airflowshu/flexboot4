package com.yunlbd.flexboot4.converter.sys;

import com.yunlbd.flexboot4.converter.BaseMapStructConfig;
import com.yunlbd.flexboot4.converter.CrudMapper;
import com.yunlbd.flexboot4.dto.sys.SysFileCreateReq;
import com.yunlbd.flexboot4.dto.sys.SysFileUpdateReq;
import com.yunlbd.flexboot4.entity.sys.SysFile;
import com.yunlbd.flexboot4.excel.sys.SysFileExportRow;
import com.yunlbd.flexboot4.vo.sys.SysFileDetailVO;
import com.yunlbd.flexboot4.vo.sys.SysFileListVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapStructConfig.class)
public interface SysFileCrudMapper extends CrudMapper<SysFile, SysFileCreateReq, SysFileUpdateReq, SysFileListVO, SysFileDetailVO> {

    @Override
    SysFile toEntity(SysFileCreateReq request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "lastModifyTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "lastModifyBy", ignore = true)
    void updateEntity(SysFileUpdateReq request, @MappingTarget SysFile entity);

    SysFileExportRow toExportRow(SysFile entity);
}
