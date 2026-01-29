package com.yunlbd.flexboot4.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yunlbd.flexboot4.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {

    /**
     * 根据文件 hash 查询（绕过 TableLogic 软删除过滤）
     */
    @Select("SELECT * FROM sys_file WHERE file_hash = #{hash} LIMIT 1")
    SysFile selectByHash(@Param("hash") String hash);

    /**
     * 恢复已删除的文件（绕过 TableLogic 软删除过滤）
     */
    @Update("UPDATE sys_file SET del_flag = 0 WHERE id = #{id}")
    int restoreById(@Param("id") String id);
}

