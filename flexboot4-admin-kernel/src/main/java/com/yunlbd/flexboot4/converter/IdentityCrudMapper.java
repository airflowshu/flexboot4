package com.yunlbd.flexboot4.converter;

/**
 * Temporary low-risk mapper used by generated/basic modules.
 * High-risk modules must provide explicit MapStruct mappers instead.
 */
public class IdentityCrudMapper<E> implements CrudMapper<E, E, E, E, E> {

    @Override
    public E toEntity(E request) {
        return request;
    }

    @Override
    public void updateEntity(E request, E entity) {
        if (request == null || entity == null) {
            return;
        }
        BeanCopyUtils.copyNonNullProperties(request, entity);
    }

    @Override
    public E toListVO(E entity) {
        return entity;
    }

    @Override
    public E toDetailVO(E entity) {
        return entity;
    }
}
