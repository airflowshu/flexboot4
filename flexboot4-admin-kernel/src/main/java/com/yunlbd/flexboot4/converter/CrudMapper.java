package com.yunlbd.flexboot4.converter;

import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * CRUD API boundary mapper.
 *
 * @param <E> entity type
 * @param <CreateReq> create request type
 * @param <UpdateReq> update request type
 * @param <ListVO> list response type
 * @param <DetailVO> detail response type
 */
public interface CrudMapper<E, CreateReq, UpdateReq, ListVO, DetailVO> {

    E toEntity(CreateReq request);

    void updateEntity(UpdateReq request, @MappingTarget E entity);

    ListVO toListVO(E entity);

    DetailVO toDetailVO(E entity);

    default List<ListVO> toListVOList(List<E> records) {
        if (records == null) {
            return List.of();
        }
        return records.stream().map(this::toListVO).toList();
    }
}
