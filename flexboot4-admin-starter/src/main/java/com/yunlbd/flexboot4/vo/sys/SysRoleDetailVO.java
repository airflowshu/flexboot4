package com.yunlbd.flexboot4.vo.sys;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleDetailVO extends SysRoleListVO {
    private List<SysMenuSimpleVO> menus;
}
