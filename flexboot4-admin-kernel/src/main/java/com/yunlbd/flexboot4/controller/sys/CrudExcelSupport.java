package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.converter.BeanCopyUtils;
import java.util.function.Function;

public class CrudExcelSupport<E, ExportRow, ImportRow> {

    private final Class<ExportRow> exportRowClass;
    private final Class<ImportRow> importRowClass;
    private final Function<E, ExportRow> exportMapper;
    private final Function<ImportRow, E> importMapper;

    private CrudExcelSupport(Class<ExportRow> exportRowClass,
                             Class<ImportRow> importRowClass,
                             Function<E, ExportRow> exportMapper,
                             Function<ImportRow, E> importMapper) {
        this.exportRowClass = exportRowClass;
        this.importRowClass = importRowClass;
        this.exportMapper = exportMapper;
        this.importMapper = importMapper;
    }

    public static <E, ExportRow, ImportRow> CrudExcelSupport<E, ExportRow, ImportRow> of(
            Class<ExportRow> exportRowClass,
            Class<ImportRow> importRowClass,
            Function<E, ExportRow> exportMapper,
            Function<ImportRow, E> importMapper) {
        return new CrudExcelSupport<>(exportRowClass, importRowClass, exportMapper, importMapper);
    }

    public static <E> CrudExcelSupport<E, E, Void> entity(Class<E> entityClass) {
        return new CrudExcelSupport<>(entityClass, Void.class, Function.identity(), null);
    }

    public Class<ExportRow> exportRowClass() {
        return exportRowClass;
    }

    public Class<ImportRow> importRowClass() {
        return importRowClass;
    }

    public boolean importEnabled() {
        return importRowClass != null && importRowClass != Void.class && importMapper != null;
    }

    public ExportRow toExportRow(E entity) {
        if (exportMapper != null) {
            return exportMapper.apply(entity);
        }
        return copy(entity, exportRowClass);
    }

    public E toEntity(ImportRow row) {
        if (!importEnabled()) {
            throw new UnsupportedOperationException("Excel导入未开启");
        }
        return importMapper.apply(row);
    }

    private static <T> T copy(Object source, Class<T> targetType) {
        if (source == null) {
            return null;
        }
        if (targetType.isInstance(source)) {
            return targetType.cast(source);
        }
        try {
            T target = targetType.getDeclaredConstructor().newInstance();
            BeanCopyUtils.copyNonNullProperties(source, target);
            return target;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法创建Excel行对象: " + targetType.getName(), e);
        }
    }
}
