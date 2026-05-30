package com.yunlbd.flexboot4.cache;

import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.common.annotation.BumpTableVersion;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

@Aspect
public class BumpTableVersionAspect {

    @AfterReturning(
            pointcut = "@annotation(com.yunlbd.flexboot4.common.annotation.BumpTableVersion)"
                    + " || execution(public * com.yunlbd.flexboot4..service..*(..))",
            returning = "result"
    )
    public void bumpAfterReturning(JoinPoint joinPoint, Object result) {
        if (!shouldBump(result)) {
            return;
        }
        BumpTableVersion annotation = findAnnotation(joinPoint);
        if (annotation == null) {
            return;
        }
        for (String table : tableNames(annotation)) {
            TableVersions.bumpVersion(table);
        }
    }

    private static boolean shouldBump(Object result) {
        return !(result instanceof Boolean ok) || ok;
    }

    private static BumpTableVersion findAnnotation(JoinPoint joinPoint) {
        if (!(joinPoint.getSignature() instanceof MethodSignature signature)) {
            return null;
        }
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget() != null ? joinPoint.getTarget().getClass() : method.getDeclaringClass();
        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        BumpTableVersion annotation = AnnotatedElementUtils.findMergedAnnotation(specificMethod, BumpTableVersion.class);
        if (annotation != null) {
            return annotation;
        }
        annotation = AnnotatedElementUtils.findMergedAnnotation(method, BumpTableVersion.class);
        if (annotation != null) {
            return annotation;
        }
        return findAnnotationOnInterfaces(targetClass, method);
    }

    private static BumpTableVersion findAnnotationOnInterfaces(Class<?> targetClass, Method method) {
        for (Class<?> ifc : targetClass.getInterfaces()) {
            BumpTableVersion annotation = findAnnotationOnInterface(ifc, method);
            if (annotation != null) {
                return annotation;
            }
        }
        Class<?> superclass = targetClass.getSuperclass();
        return superclass != null && superclass != Object.class ? findAnnotationOnInterfaces(superclass, method) : null;
    }

    private static BumpTableVersion findAnnotationOnInterface(Class<?> ifc, Method method) {
        try {
            Method interfaceMethod = ifc.getMethod(method.getName(), method.getParameterTypes());
            BumpTableVersion annotation = AnnotatedElementUtils.findMergedAnnotation(interfaceMethod, BumpTableVersion.class);
            if (annotation != null) {
                return annotation;
            }
        } catch (NoSuchMethodException ignored) {
        }
        for (Class<?> parent : ifc.getInterfaces()) {
            BumpTableVersion annotation = findAnnotationOnInterface(parent, method);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    private static Set<String> tableNames(BumpTableVersion annotation) {
        Set<String> names = new LinkedHashSet<>();
        for (Class<?> entity : annotation.entities()) {
            addIfPresent(names, tableName(entity));
        }
        for (String table : annotation.tables()) {
            addIfPresent(names, table);
        }
        return names;
    }

    private static void addIfPresent(Set<String> names, String table) {
        if (table != null && !table.isBlank()) {
            names.add(table);
        }
    }

    private static String tableName(Class<?> entityClass) {
        if (entityClass == null) {
            return null;
        }
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && table.value() != null && !table.value().isBlank()) {
            return table.value();
        }
        return entityClass.getSimpleName();
    }
}
