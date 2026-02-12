package com.yunlbd.flexboot4.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yunlbd.flexboot4.entity.kb.KbMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysKbMemberMapper extends BaseMapper<KbMember> {

    @Select("SELECT * FROM kb_member WHERE kb_id = #{kbId} AND user_id = #{userId} LIMIT 1")
    KbMember selectByKbAndUser(@Param("kbId") String kbId, @Param("userId") String userId);

    @Update("UPDATE kb_member SET del_flag = 0 WHERE id = #{id}")
    int restoreById(@Param("id") String id);
}
