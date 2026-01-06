package com.yunlbd.flexboot4.service.impl;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.CacheableServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 通用业务层实现基类，封装了带缓存的CRUD操作
 * 
 * 注意：需要在子类上使用 @CacheConfig(cacheNames = "xxx") 指定缓存名称。
 * 父类方法使用了 cacheResolver = "dynamicCacheResolver" 来动态获取子类配置的缓存名称。
 */
public class BaseServiceImpl<M extends BaseMapper<T>, T> extends CacheableServiceImpl<M, T> {

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean save(T entity) {
        return super.save(entity);
    }

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean saveBatch(Collection<T> entities, int batchSize) {
        return super.saveBatch(entities, batchSize);
    }

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean saveOrUpdate(T entity) {
        return super.saveOrUpdate(entity);
    }

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean remove(QueryWrapper query) {
        return super.remove(query);
    }

    @Override
    @CacheEvict(key = "#id", cacheResolver = "dynamicCacheResolver")
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean removeByIds(Collection<? extends Serializable> ids) {
        return super.removeByIds(ids);
    }

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean update(T entity, QueryWrapper query) {
        return super.update(entity, query);
    }

    @Override
    @CacheEvict(key = "#entity.id", cacheResolver = "dynamicCacheResolver")
    public boolean updateById(T entity, boolean ignoreNulls) {
        return super.updateById(entity, ignoreNulls);
    }

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean updateBatch(Collection<T> entities, int batchSize) {
        return super.updateBatch(entities, batchSize);
    }

    @Override
    @Cacheable(key = "#id", cacheResolver = "dynamicCacheResolver")
    public T getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    @Cacheable(key = "#root.methodName + ':' + #query.toSQL()", cacheResolver = "dynamicCacheResolver")
    public T getOne(QueryWrapper query) {
        return super.getOne(query);
    }

    @Override
    @Cacheable(key = "#root.methodName + ':' + #query.toSQL()", cacheResolver = "dynamicCacheResolver")
    public <R> R getOneAs(QueryWrapper query, Class<R> asType) {
        return super.getOneAs(query, asType);
    }

    @Override
    @Cacheable(key = "#root.methodName + ':' + #query.toSQL()", cacheResolver = "dynamicCacheResolver")
    public List<T> list(QueryWrapper query) {
        return super.list(query);
    }

    @Override
    @Cacheable(key = "#root.methodName + ':' + #query.toSQL()", cacheResolver = "dynamicCacheResolver")
    public <R> List<R> listAs(QueryWrapper query, Class<R> asType) {
        return super.listAs(query, asType);
    }

    @Override
    @Deprecated
    public List<T> listByIds(Collection<? extends Serializable> ids) {
        return super.listByIds(ids);
    }

    @Override
    @Cacheable(key = "#root.methodName + ':' + #query.toSQL()", cacheResolver = "dynamicCacheResolver")
    public long count(QueryWrapper query) {
        return super.count(query);
    }

    @Override
    @Cacheable(key = "#root.methodName + ':' + #page.getPageSize() + ':' + #page.getPageNumber() + ':' + #query.toSQL()", cacheResolver = "dynamicCacheResolver")
    public Page<T> page(Page<T> page, QueryWrapper query) {
        return super.page(page, query);
    }

    @Override
    @Cacheable(key = "#root.methodName + ':' + #page.getPageSize() + ':' + #page.getPageNumber() + ':' + #query.toSQL()", cacheResolver = "dynamicCacheResolver")
    public <R> Page<R> pageAs(Page<R> page, QueryWrapper query, Class<R> asType) {
        return super.pageAs(page, query, asType);
    }
}
