package com.yunlbd.flexboot4.excel;

import java.util.Objects;
import java.util.function.BiFunction;

public final class DictTextResolver {
    private static volatile BiFunction<String, Object, String> provider = (dict, code) -> Objects.toString(code, null);
    public static String resolve(String dict, Object code) {
        return provider.apply(dict, code);
    }
    public static void setProvider(BiFunction<String, Object, String> p) {
        provider = p == null ? provider : p;
    }
}

