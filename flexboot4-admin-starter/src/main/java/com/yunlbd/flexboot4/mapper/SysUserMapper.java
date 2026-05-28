package com.yunlbd.flexboot4.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Update("""
            update sys_user
            set real_name = #{realName},
                profile_file_id = #{profileFileId},
                remark = #{remark},
                last_modify_time = now()
            where id = #{id}
              and del_flag = 0
            """)
    int updateCurrentProfile(@Param("id") String id,
                             @Param("realName") String realName,
                             @Param("profileFileId") String profileFileId,
                             @Param("remark") String remark);
}
