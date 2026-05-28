package com.yunlbd.flexboot4.vo.ops;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictTypeDetailVO extends SysDictTypeListVO {
    private List<SysDictItemListVO> dictItems;
}
