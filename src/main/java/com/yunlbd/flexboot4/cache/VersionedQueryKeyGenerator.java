package com.yunlbd.flexboot4.cache;

import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.jspecify.annotations.NullMarked;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.StringJoiner;

public class VersionedQueryKeyGenerator implements KeyGenerator {
    @Override
    @NullMarked
    public Object generate(Object target, Method method, Object... params) {
        StringJoiner sj = new StringJoiner(":");
        sj.add(method.getName());

        Page<?> page = null;
        QueryWrapper query = null;
        Object id = null;

        for (Object p : params) {
            if (p instanceof Page<?> pg) {
                page = pg;
            } else if (p instanceof QueryWrapper qw) {
                query = qw;
            } else if (p != null && id == null) {
                id = p;
            }
        }

        if (page != null) {
            sj.add(String.valueOf(page.getPageSize()));
            sj.add(String.valueOf(page.getPageNumber()));
        }

        String sql = query != null ? query.toSQL() : null;
        Set<String> tables = SqlTableNameExtractor.extractTables(sql);
        
        // 如果没有从SQL中提取到表名，尝试从目标类推断实体表名
        if (tables == null || tables.isEmpty()) {
            tables = Set.of(inferTableName(target));
        }
        
        sj.add(versionPart(tables));

        if (id != null && query == null) {
            sj.add(String.valueOf(id));
        } else if (sql != null) {
            sj.add(sql);
        }

        return sj.toString();
    }

    private static String versionPart(Set<String> tables) {
        if (tables == null || tables.isEmpty()) {
            return "ver0";
        }
        StringJoiner sj = new StringJoiner("|", "ver:", "");
        for (String t : tables) {
            sj.add(t + "=" + TableVersions.getVersion(t));
        }
        return sj.toString();
    }
    
    private static String inferTableName(Object target) {
        // 从目标对象的泛型信息或实体类的@Table注解推断表名
        Class<?> targetClass = target.getClass();
        
        // 查找继承链中泛型参数为实体类的类
        while (targetClass != null && targetClass != Object.class) {
            Class<?> superClass = targetClass.getSuperclass();
            if (superClass != null && superClass.getName().contains("BaseServiceImpl")) {
                // 尝试通过反射获取泛型参数对应的实体类
                try {
                    java.lang.reflect.Type genericSuperclass = targetClass.getGenericSuperclass();
                    if (genericSuperclass instanceof java.lang.reflect.ParameterizedType pType) {
                        java.lang.reflect.Type[] actualTypeArguments = pType.getActualTypeArguments();
                        if (actualTypeArguments.length > 1) {
                            Class<?> entityClass = (Class<?>) actualTypeArguments[1];
                            Table tableAnnotation = entityClass.getAnnotation(Table.class);
                            if (tableAnnotation != null && tableAnnotation.value() != null) {
                                return tableAnnotation.value();
                            }
                            return entityClass.getSimpleName();
                        }
                    }
                } catch (Exception e) {
                    // 如果反射失败，回退到其他方法
                }
            }
            targetClass = targetClass.getSuperclass();
        }
        
        // 如果无法推断，返回一个默认值
        return "unknown";
    }
}

