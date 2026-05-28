package com.yunlbd.flexboot4.vo.sys;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysMenuDetailVO extends SysMenuListVO {
    private SysMenuSimpleVO parent;
}
