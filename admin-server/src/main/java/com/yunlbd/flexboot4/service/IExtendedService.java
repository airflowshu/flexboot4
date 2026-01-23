package com.yunlbd.flexboot4.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.yunlbd.flexboot4.dto.SearchDto;

import java.util.List;

/**
 * 扩展的服务接口，增加带关系查询的分页方法
 * @param <T> 实体类型
 */
public interface IExtendedService<T> extends IService<T> {
    
    /**
     * 带关系查询的分页查询方法
     * @param searchDto 查询参数
     * @return 分页结果
     */
    Page<T> pageWithRelations(SearchDto searchDto);
    
    /**
     * 带关系查询的列表查询方法
     * @param searchDto 查询参数
     * @return 列表结果
     */
    List<T> listWithRelations(SearchDto searchDto);
}