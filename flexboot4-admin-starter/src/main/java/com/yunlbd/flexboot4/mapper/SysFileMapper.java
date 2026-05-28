package com.yunlbd.flexboot4.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yunlbd.flexboot4.entity.sys.SysFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {

    /**
     * 根据文件 hash 查询（绕过 TableLogic 软删除过滤）
     */
    @Select("SELECT * FROM sys_file WHERE file_hash = #{hash} AND del_flag = 0 LIMIT 1")
    SysFile selectByHash(@Param("hash") String hash);
}

