package com.yunlbd.flexboot4.converter;

import com.yunlbd.flexboot4.dto.ops.SysDictTypeCreateReq;
import com.yunlbd.flexboot4.dto.ops.SysDictTypeUpdateReq;
import com.yunlbd.flexboot4.entity.ops.SysDictItem;
import com.yunlbd.flexboot4.entity.ops.SysDictType;
import com.yunlbd.flexboot4.vo.ops.SysDictTypeDetailVO;
import com.yunlbd.flexboot4.vo.ops.SysDictTypeListVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultCrudMapperTest {

    private final DefaultCrudMapper<SysDictType, SysDictTypeCreateReq, SysDictTypeUpdateReq,
            SysDictTypeListVO, SysDictTypeDetailVO> mapper = new DefaultCrudMapper<>(
            SysDictType.class,
            SysDictTypeListVO.class,
            SysDictTypeDetailVO.class
    );

    @Test
    void toListVO_ignoresEntityRelationFieldsMissingFromListVO() {
        SysDictType entity = dictTypeWithItems();

        SysDictTypeListVO vo = assertDoesNotThrow(() -> mapper.toListVO(entity));

        assertEquals("gender", vo.getCode());
        assertEquals("性别", vo.getName());
    }

    @Test
    void toDetailVO_mapsRelationFieldsWhenDetailVODeclaresThem() {
        SysDictType entity = dictTypeWithItems();

        SysDictTypeDetailVO vo = mapper.toDetailVO(entity);

        assertNotNull(vo.getDictItems());
        assertEquals(1, vo.getDictItems().size());
        assertEquals("male", vo.getDictItems().getFirst().getItemCode());
    }

    private static SysDictType dictTypeWithItems() {
        return SysDictType.builder()
                .id("1")
                .code("gender")
                .name("性别")
                .dictItems(List.of(SysDictItem.builder()
                        .id("10")
                        .itemCode("male")
                        .itemText("男")
                        .build()))
                .build();
    }
}
