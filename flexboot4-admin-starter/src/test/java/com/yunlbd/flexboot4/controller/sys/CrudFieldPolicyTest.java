package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrudFieldPolicyTest {

    @Test
    void validate_allowsRootEntityAliasForWhitelistedFields() {
        SearchDto dto = new SearchDto();
        dto.setItems(List.of(item("sysUser.username", "like", "admin")));
        dto.setOrders(List.of(order("sysUser.createTime", false)));

        CrudFieldPolicy policy = CrudFieldPolicy.same(List.of("username", "createTime"));

        assertDoesNotThrow(() -> policy.validate(dto, SysUser.class));
    }

    @Test
    void validate_stillRejectsRelationFieldsWithoutExplicitWhitelist() {
        SearchDto dto = new SearchDto();
        dto.setOrders(List.of(order("dept.createTime", false)));

        CrudFieldPolicy policy = CrudFieldPolicy.same(List.of("username", "createTime"));

        assertThrows(IllegalArgumentException.class, () -> policy.validate(dto, SysUser.class));
    }

    @Test
    void validate_allowsExplicitRelationFieldWhitelist() {
        SearchDto dto = new SearchDto();
        dto.setItems(List.of(item("sysDept.deptName", "like", "研发")));

        CrudFieldPolicy policy = CrudFieldPolicy.same(List.of("username", "createTime"))
                .withQueryFields("dept.deptName");

        assertDoesNotThrow(() -> policy.validate(dto, SysUser.class));
    }

    private static SearchDto.SearchItem item(String field, String op, Object val) {
        SearchDto.SearchItem item = new SearchDto.SearchItem();
        item.setField(field);
        item.setOp(op);
        item.setVal(val);
        return item;
    }

    private static SearchDto.OrderItem order(String column, boolean asc) {
        SearchDto.OrderItem order = new SearchDto.OrderItem();
        order.setColumn(column);
        order.setAsc(asc);
        return order;
    }
}
