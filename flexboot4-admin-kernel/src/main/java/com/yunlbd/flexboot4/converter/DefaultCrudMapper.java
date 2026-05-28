package com.yunlbd.flexboot4.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class DefaultCrudMapper<E, CreateReq, UpdateReq, ListVO, DetailVO>
        implements CrudMapper<E, CreateReq, UpdateReq, ListVO, DetailVO> {

    private final Class<E> entityClass;
    private final Class<ListVO> listVOClass;
    private final Class<DetailVO> detailVOClass;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public DefaultCrudMapper(Class<E> entityClass, Class<ListVO> listVOClass, Class<DetailVO> detailVOClass) {
        this.entityClass = entityClass;
        this.listVOClass = listVOClass;
        this.detailVOClass = detailVOClass;
    }

    @Override
    public E toEntity(CreateReq request) {
        E entity = instantiate(entityClass);
        BeanCopyUtils.copyNonNullProperties(request, entity);
        return entity;
    }

    @Override
    public void updateEntity(UpdateReq request, E entity) {
        BeanCopyUtils.copyNonNullProperties(request, entity);
    }

    @Override
    public ListVO toListVO(E entity) {
        return convert(entity, listVOClass);
    }

    @Override
    public DetailVO toDetailVO(E entity) {
        return convert(entity, detailVOClass);
    }

    private <T> T convert(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        return objectMapper.convertValue(source, targetClass);
    }

    private static <T> T instantiate(Class<T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法创建对象: " + type.getName(), e);
        }
    }
}
