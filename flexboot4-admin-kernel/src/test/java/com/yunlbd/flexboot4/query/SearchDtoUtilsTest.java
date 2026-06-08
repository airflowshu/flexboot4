package com.yunlbd.flexboot4.query;

import com.mybatisflex.annotation.RelationManyToMany;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.dto.SearchDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchDtoUtilsTest {

    @Test
    void hasQualifiedPathsRecognizesRootEntityQualifier() {
        SearchDto dto = SearchDtoUtils.create(
                1,
                10,
                "AND",
                List.of(SearchDto.item("sysUser.username", "like", "super")),
                List.of(order("sysUser.createTime"))
        );

        assertThat(SearchDtoUtils.hasQualifiedPaths(dto)).isTrue();
        assertThat(SearchDtoUtils.hasRelationPaths(dto, TestUser.class)).isFalse();
    }

    @Test
    void hasQualifiedPathsIgnoresUnqualifiedFields() {
        SearchDto dto = SearchDtoUtils.create(
                1,
                10,
                "AND",
                List.of(SearchDto.item("username", "like", "super")),
                List.of(order("createTime"))
        );

        assertThat(SearchDtoUtils.hasQualifiedPaths(dto)).isFalse();
        assertThat(SearchDtoUtils.hasRelationPaths(dto, TestUser.class)).isFalse();
    }

    @Test
    void hasRelationPathsStillRecognizesRelationQualifier() {
        SearchDto dto = SearchDtoUtils.create(
                1,
                10,
                "AND",
                List.of(SearchDto.item("roles.roleName", "like", "管理员")),
                List.of()
        );

        assertThat(SearchDtoUtils.hasQualifiedPaths(dto)).isTrue();
        assertThat(SearchDtoUtils.hasRelationPaths(dto, TestUser.class)).isTrue();
    }

    private static SearchDto.OrderItem order(String column) {
        SearchDto.OrderItem item = new SearchDto.OrderItem();
        item.setColumn(column);
        return item;
    }

    @Table("sys_user")
    static class TestUser {
        @RelationManyToMany(
                joinTable = "sys_user_role",
                selfField = "id", joinSelfColumn = "user_id",
                targetField = "id", joinTargetColumn = "role_id"
        )
        private List<TestRole> roles;
    }

    @Table("sys_role")
    static class TestRole {
    }
}
