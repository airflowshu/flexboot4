package com.yunlbd.flexboot4.excel;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.List;

@SuppressWarnings("all")
public final class ReactiveExportSupport {
    @SuppressWarnings("unchecked")
    public static <T, R> Flux<R> queryFlux(IService<T> service, QueryWrapper query, Class<R> asType, int pageSize) {
        Method m = findReactiveListAs(service);
        if (m != null) {
            try {
                Object r = m.invoke(service, query, asType);
                if (r instanceof Flux) {
                    return (Flux<R>) r;
                }
            } catch (Throwable ignore) {
            }
        }
        return Flux.create(emitter -> {
            int pageNo = 1;
            while (true) {
                Page<R> p = service.pageAs(new Page<>(pageNo, pageSize), query, asType);
                List<R> records = p.getRecords();
                if (records == null || records.isEmpty()) {
                    break;
                }
                for (R item : records) {
                    emitter.next(item);
                }
                pageNo++;
            }
            emitter.complete();
        });
    }

    private static Method findReactiveListAs(Object service) {
        for (Method m : service.getClass().getMethods()) {
            if (m.getName().equals("listAs")
                && m.getParameterCount() == 2
                && m.getParameterTypes()[0] == QueryWrapper.class
                && Flux.class.isAssignableFrom(m.getReturnType())) {
                return m;
            }
        }
        return null;
    }
}

