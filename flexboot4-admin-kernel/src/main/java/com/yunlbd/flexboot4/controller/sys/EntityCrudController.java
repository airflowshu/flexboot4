package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.converter.DefaultCrudMapper;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

import java.io.Serializable;

/**
 * Low-code CRUD bridge for modules generated with simple DTO/VO contracts.
 */
public abstract class EntityCrudController<S extends IExtendedService<E>, E, ID extends Serializable,
        CreateReq, UpdateReq, ListVO, DetailVO>
        extends BaseCrudController<S, E, ID, CreateReq, UpdateReq, ListVO, DetailVO> {

    protected EntityCrudController(S service,
                                   Class<E> entityClass,
                                   Class<ListVO> listVOClass,
                                   Class<DetailVO> detailVOClass) {
        super(service, new DefaultCrudMapper<>(entityClass, listVOClass, detailVOClass));
    }
}
