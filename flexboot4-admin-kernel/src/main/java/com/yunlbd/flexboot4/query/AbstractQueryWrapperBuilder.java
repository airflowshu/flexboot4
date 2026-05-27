package com.yunlbd.flexboot4.query;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.SearchDto;

public abstract class AbstractQueryWrapperBuilder {
    public abstract QueryWrapper build(SearchDto dto, Class<?> entityClass);
}
