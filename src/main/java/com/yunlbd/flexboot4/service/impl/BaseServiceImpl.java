package com.yunlbd.flexboot4.service.impl;

import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.relation.RelationManager;
import com.mybatisflex.spring.service.impl.CacheableServiceImpl;
import com.yunlbd.flexboot4.cache.TableVersions;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.BaseEntity;
import com.yunlbd.flexboot4.query.DefaultQueryWrapperBuilder;
import com.yunlbd.flexboot4.query.SearchDtoUtils;
import com.yunlbd.flexboot4.service.IExtendedService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ResolvableType;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 通用业务层实现基类，封装了带缓存的CRUD操作
 * 
 * 注意：需要在子类上使用 @CacheConfig(cacheNames = "xxx") 指定缓存名称。
 * 父类方法使用了 cacheResolver = "dynamicCacheResolver" 来动态获取子类配置的缓存名称。
 */
public class BaseServiceImpl<M extends BaseMapper<T>, T extends BaseEntity> extends CacheableServiceImpl<M, T> implements IExtendedService<T> {

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean save(T entity) {
        boolean ok = super.save(entity);
        if (ok) {
            bumpVersionsOnWrite(entity != null ? entity.getClass() : resolveEntityClass());
        }
        return ok;
    }

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean saveBatch(Collection<T> entities, int batchSize) {
        boolean ok = super.saveBatch(entities, batchSize);
        if (ok) {
            Class<?> c = firstEntityClass(entities);
            bumpVersionsOnWrite(c != null ? c : resolveEntityClass());
        }
        return ok;
    }

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean saveOrUpdate(T entity) {
        boolean ok = super.saveOrUpdate(entity);
        if (ok) {
            bumpVersionsOnWrite(entity != null ? entity.getClass() : resolveEntityClass());
        }
        return ok;
    }

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean remove(QueryWrapper query) {
        boolean ok = super.remove(query);
        if (ok) {
            bumpVersionsOnWrite(resolveEntityClass());
        }
        return ok;
    }

    @Override
    @CacheEvict(key = "#id", cacheResolver = "dynamicCacheResolver")
    public boolean removeById(Serializable id) {
        boolean ok = super.removeById(id);
        if (ok) {
            bumpVersionsOnWrite(resolveEntityClass());
        }
        return ok;
    }

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean removeByIds(Collection<? extends Serializable> ids) {
        boolean ok = super.removeByIds(ids);
        if (ok) {
            bumpVersionsOnWrite(resolveEntityClass());
        }
        return ok;
    }

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean update(T entity, QueryWrapper query) {
        boolean ok = super.update(entity, query);
        if (ok) {
            bumpVersionsOnWrite(entity != null ? entity.getClass() : resolveEntityClass());
        }
        return ok;
    }

    @Override
    @CacheEvict(key = "#entity.id", cacheResolver = "dynamicCacheResolver")
    public boolean updateById(T entity, boolean ignoreNulls) {
        boolean ok = super.updateById(entity, ignoreNulls);
        if (ok) {
            bumpVersionsOnWrite(entity != null ? entity.getClass() : resolveEntityClass());
        }
        return ok;
    }

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean updateBatch(Collection<T> entities, int batchSize) {
        boolean ok = super.updateBatch(entities, batchSize);
        if (ok) {
            Class<?> c = firstEntityClass(entities);
            bumpVersionsOnWrite(c != null ? c : resolveEntityClass());
        }
        return ok;
    }

    @Override
    @Cacheable(keyGenerator = "versionedQueryKeyGenerator", cacheResolver = "dynamicCacheResolver")
    public T getById(Serializable id) {
        T entity = super.getById(id);
        if (entity != null) {
            RelationManager.queryRelations(super.getMapper(), List.of(entity));
        }
        return entity;
    }

    @Override
    @Cacheable(keyGenerator = "versionedQueryKeyGenerator", cacheResolver = "dynamicCacheResolver")
    public T getOne(QueryWrapper query) {
        return super.getOne(query);
    }

    @Override
    @Cacheable(keyGenerator = "versionedQueryKeyGenerator", cacheResolver = "dynamicCacheResolver")
    public <R> R getOneAs(QueryWrapper query, Class<R> asType) {
        return super.getOneAs(query, asType);
    }

    @Override
    @Cacheable(keyGenerator = "versionedQueryKeyGenerator", cacheResolver = "dynamicCacheResolver")
    public List<T> list(QueryWrapper query) {
        return super.list(query);
    }

    @Override
    @Cacheable(keyGenerator = "versionedQueryKeyGenerator", cacheResolver = "dynamicCacheResolver")
    public <R> List<R> listAs(QueryWrapper query, Class<R> asType) {
        return super.listAs(query, asType);
    }

    @Override
    @Deprecated
    public List<T> listByIds(Collection<? extends Serializable> ids) {
        return super.listByIds(ids);
    }

    @Override
    @Cacheable(keyGenerator = "versionedQueryKeyGenerator", cacheResolver = "dynamicCacheResolver")
    public long count(QueryWrapper query) {
        return super.count(query);
    }

    @Override
    @Cacheable(keyGenerator = "versionedQueryKeyGenerator", cacheResolver = "dynamicCacheResolver")
    public Page<T> page(Page<T> page, QueryWrapper query) {
        return super.page(page, query);
    }

    @Override
    @Cacheable(keyGenerator = "versionedQueryKeyGenerator", cacheResolver = "dynamicCacheResolver")
    public <R> Page<R> pageAs(Page<R> page, QueryWrapper query, Class<R> asType) {
        return super.pageAs(page, query, asType);
    }

    /**
     * 带关系查询的分页查询方法
     */
    @Cacheable(keyGenerator = "versionedQueryKeyGenerator", cacheResolver = "dynamicCacheResolver")
    public Page<T> pageWithRelations(SearchDto searchDto) {
        Page<T> page = new Page<>(searchDto.getPageNumber(), searchDto.getPageSize());
        QueryWrapper queryWrapper = DefaultQueryWrapperBuilder.get().build(searchDto, resolveEntityClass());
        Page<T> result = super.page(page, queryWrapper);
        if (SearchDtoUtils.hasRelationPaths(searchDto)) {
            RelationManager.queryRelations(getMapper(), result.getRecords());
        }
        return result;
    }

    /**
     * 带关系查询的列表查询方法
     */
    @Cacheable(keyGenerator = "versionedQueryKeyGenerator", cacheResolver = "dynamicCacheResolver")
    public List<T> listWithRelations(SearchDto searchDto) {
        QueryWrapper queryWrapper = DefaultQueryWrapperBuilder.get().build(searchDto, resolveEntityClass());
        List<T> result = super.list(queryWrapper);
        if (SearchDtoUtils.hasRelationPaths(searchDto)) {
            RelationManager.queryRelations(getMapper(), result);
        }
        return result;
    }

    protected Collection<String> extraInvalidateTables() {
        return Collections.emptyList();
    }

    private void bumpVersionsOnWrite(Class<?> entityClass) {
        String base = tableName(entityClass);
        if (base != null && !base.isBlank()) {
            TableVersions.bumpVersion(base);
        }
        for (String t : extraInvalidateTables()) {
            if (t != null && !t.isBlank()) {
                TableVersions.bumpVersion(t);
            }
        }
    }

    private static Class<?> firstEntityClass(Collection<?> entities) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }
        Object first = entities.iterator().next();
        return first != null ? first.getClass() : null;
    }

    private Class<?> resolveEntityClass() {
        try {
            return ResolvableType.forClass(getClass()).as(BaseServiceImpl.class).getGeneric(1).resolve();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String tableName(Class<?> entityClass) {
        if (entityClass == null) {
            return null;
        }
        Table t = entityClass.getAnnotation(Table.class);
        if (t != null && t.value() != null && !t.value().isBlank()) {
            return t.value();
        }
        return entityClass.getSimpleName();
    }
}
