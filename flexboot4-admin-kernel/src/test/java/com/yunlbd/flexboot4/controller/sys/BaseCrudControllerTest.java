package com.yunlbd.flexboot4.controller.sys;

import com.mybatisflex.core.paginate.Page;
import com.yunlbd.flexboot4.converter.CrudMapper;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import com.yunlbd.flexboot4.service.sys.IExtendedService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseCrudControllerTest {

    private final TestService service = mock(TestService.class);
    private final TestController controller = new TestController(service);

    @Test
    void create_mapsCreateReqToEntityBeforeSaving() {
        when(service.save(any())).thenReturn(true);

        var result = controller.create(new CreateReq("alpha", 1));

        ArgumentCaptor<TestEntity> captor = ArgumentCaptor.forClass(TestEntity.class);
        verify(service).save(captor.capture());
        assertThat(result.getData()).isTrue();
        assertThat(captor.getValue().getName()).isEqualTo("alpha");
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    void update_ignoresNullFieldsAndKeepsExistingValues() {
        TestEntity existing = new TestEntity();
        existing.setId("42");
        existing.setName("old");
        existing.setStatus(1);
        when(service.getById("42")).thenReturn(existing);
        when(service.updateById(any(), eq(true))).thenReturn(true);

        var result = controller.update("42", new UpdateReq(null, 0));

        ArgumentCaptor<TestEntity> captor = ArgumentCaptor.forClass(TestEntity.class);
        verify(service).updateById(captor.capture(), eq(true));
        assertThat(result.getData()).isTrue();
        assertThat(captor.getValue().getId()).isEqualTo("42");
        assertThat(captor.getValue().getName()).isEqualTo("old");
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
    }

    @Test
    void page_mapsEntityRecordsToListVO() {
        SearchDto searchDto = new SearchDto();
        TestEntity entity = new TestEntity();
        entity.setId("1");
        entity.setName("alpha");
        entity.setStatus(1);
        Page<TestEntity> source = new Page<>(1, 10);
        source.setTotalRow(1);
        source.setRecords(List.of(entity));
        when(service.pageWithRelations(searchDto)).thenReturn(source);

        var result = controller.page(searchDto);

        assertThat(result.getData().getTotalRow()).isEqualTo(1);
        assertThat(result.getData().getRecords()).singleElement()
                .extracting(ListVO::name)
                .isEqualTo("alpha");
    }

    @Test
    void list_mapsEntityRecordsToListVO() {
        SearchDto searchDto = new SearchDto();
        TestEntity entity = new TestEntity();
        entity.setName("beta");
        when(service.listWithRelations(searchDto)).thenReturn(List.of(entity));

        var result = controller.list(searchDto);

        assertThat(result.getData()).singleElement()
                .extracting(ListVO::name)
                .isEqualTo("beta");
    }

    interface TestService extends IExtendedService<TestEntity> {
    }

    static class TestController extends BaseCrudController<TestService, TestEntity, String,
            CreateReq, UpdateReq, ListVO, DetailVO> {

        TestController(TestService service) {
            super(service, new TestMapper());
        }

        @Override
        public Class<TestEntity> getEntityClass() {
            return TestEntity.class;
        }

        @Override
        protected CrudFieldPolicy fieldPolicy() {
            return CrudFieldPolicy.same(List.of("id", "name", "status", "createTime"));
        }
    }

    static class TestEntity extends BaseEntity {
        private String name;
        private Integer status;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    record CreateReq(String name, Integer status) {
    }

    record UpdateReq(String name, Integer status) {
    }

    record ListVO(String id, String name, Integer status) {
    }

    record DetailVO(String id, String name, Integer status) {
    }

    static class TestMapper implements CrudMapper<TestEntity, CreateReq, UpdateReq, ListVO, DetailVO> {
        @Override
        public TestEntity toEntity(CreateReq request) {
            TestEntity entity = new TestEntity();
            entity.setName(request.name());
            entity.setStatus(request.status());
            return entity;
        }

        @Override
        public void updateEntity(UpdateReq request, TestEntity entity) {
            if (request.name() != null) {
                entity.setName(request.name());
            }
            if (request.status() != null) {
                entity.setStatus(request.status());
            }
        }

        @Override
        public ListVO toListVO(TestEntity entity) {
            return new ListVO(entity.getId(), entity.getName(), entity.getStatus());
        }

        @Override
        public DetailVO toDetailVO(TestEntity entity) {
            return new DetailVO(entity.getId(), entity.getName(), entity.getStatus());
        }
    }
}
