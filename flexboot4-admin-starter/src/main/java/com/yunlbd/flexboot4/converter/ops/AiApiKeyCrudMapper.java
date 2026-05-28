package com.yunlbd.flexboot4.converter.ops;

import com.yunlbd.flexboot4.converter.BaseMapStructConfig;
import com.yunlbd.flexboot4.converter.CrudMapper;
import com.yunlbd.flexboot4.dto.ops.AiApiKeyCreateReq;
import com.yunlbd.flexboot4.dto.ops.AiApiKeyUpdateReq;
import com.yunlbd.flexboot4.entity.ops.AiApiKey;
import com.yunlbd.flexboot4.excel.ops.AiApiKeyExportRow;
import com.yunlbd.flexboot4.vo.ops.AiApiKeyDetailVO;
import com.yunlbd.flexboot4.vo.ops.AiApiKeyListVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapStructConfig.class)
public interface AiApiKeyCrudMapper extends CrudMapper<AiApiKey, AiApiKeyCreateReq, AiApiKeyUpdateReq, AiApiKeyListVO, AiApiKeyDetailVO> {

    @Override
    @Mapping(target = "apiKey", ignore = true)
    @Mapping(target = "used", ignore = true)
    @Mapping(target = "lastUsedTime", ignore = true)
    @Mapping(target = "user", ignore = true)
    AiApiKey toEntity(AiApiKeyCreateReq request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "lastModifyTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "lastModifyBy", ignore = true)
    @Mapping(target = "apiKey", ignore = true)
    @Mapping(target = "used", ignore = true)
    @Mapping(target = "lastUsedTime", ignore = true)
    @Mapping(target = "statusStr", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntity(AiApiKeyUpdateReq request, @MappingTarget AiApiKey entity);

    AiApiKeyExportRow toExportRow(AiApiKey entity);
}
