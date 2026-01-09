package com.yunlbd.flexboot4.util;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ExcelExportUtil {
    private static final Logger log = LoggerFactory.getLogger(ExcelExportUtil.class);
    private static final ObjectMapper om = new ObjectMapper();

    public static <R> File writeFluxToTempFile(Flux<R> flux, Class<R> modelClass, int bufferSize) {
        try {
            File temp = File.createTempFile("export_", ".xlsx");
            List<Field> fields = collectFields(modelClass);
            List<List<String>> head = fields.stream()
                .map(f -> List.of(f.getName()))
                .collect(Collectors.toList());
            var writer = EasyExcel.write(temp).build();
            var sheet = EasyExcel.writerSheet(1).head(head).build();
            flux.buffer(bufferSize)
                .doOnNext(list -> {
                    List<List<Object>> rows = new ArrayList<>(list.size());
                    for (R item : list) {
                        rows.add(toRow(item, fields));
                    }
                    writer.write(rows, sheet);
                })
                .then()
                .doFinally(s -> {
                    try {
                        writer.finish();
                    } catch (Throwable ignore) {
                    }
                })
                .block();
            return temp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void streamFileWithRange(HttpServletRequest request, HttpServletResponse response, File file, String downloadName) {
        String encoded = URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");
        String contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        response.setContentType(contentType);
        long length = file.length();
        String range = request.getHeader("Range");
        if (range != null && range.startsWith("bytes=")) {
            long[] se = parseRange(range, length);
            long start = se[0];
            long end = se[1];
            long contentLength = end - start + 1;
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
            response.setHeader("Content-Length", String.valueOf(contentLength));
            try (RandomAccessFile raf = new RandomAccessFile(file, "r");
                 OutputStream os = response.getOutputStream()) {
                raf.seek(start);
                byte[] buf = new byte[8192];
                long remaining = contentLength;
                while (remaining > 0) {
                    int read = raf.read(buf, 0, (int) Math.min(buf.length, remaining));
                    if (read == -1) break;
                    os.write(buf, 0, read);
                    remaining -= read;
                }
                os.flush();
            } catch (IOException e) {
                log.error("Stream range error", e);
                throw new RuntimeException(e);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Content-Length", String.valueOf(length));
            try (RandomAccessFile raf = new RandomAccessFile(file, "r");
                 OutputStream os = response.getOutputStream()) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = raf.read(buf)) != -1) {
                    os.write(buf, 0, read);
                }
                os.flush();
            } catch (IOException e) {
                log.error("Stream file error", e);
                throw new RuntimeException(e);
            }
        }
    }

    private static long[] parseRange(String range, long length) {
        String v = range.substring("bytes=".length());
        String[] parts = v.split("-", 2);
        long start = parts[0].isEmpty() ? 0 : Long.parseLong(parts[0]);
        long end = parts.length > 1 && !parts[1].isEmpty() ? Long.parseLong(parts[1]) : length - 1;
        if (start < 0) start = 0;
        if (end >= length) end = length - 1;
        if (start > end) {
            start = 0;
            end = length - 1;
        }
        return new long[]{start, end};
    }

    private static List<Field> collectFields(Class<?> clazz) {
        Set<String> names = new LinkedHashSet<>();
        List<Field> all = new ArrayList<>();
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if ((f.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0) continue;
                if ((f.getModifiers() & java.lang.reflect.Modifier.TRANSIENT) != 0) continue;
                if (!names.add(f.getName())) continue;
                f.setAccessible(true);
                all.add(f);
            }
            c = c.getSuperclass();
        }
        return all;
    }

    private static List<Object> toRow(Object obj, List<Field> fields) {
        List<Object> row = new ArrayList<>(fields.size());
        for (Field f : fields) {
            Object val;
            try {
                val = f.get(obj);
            } catch (Throwable e) {
                val = null;
            }
            if (val == null) {
                row.add(null);
            } else {
                Class<?> t = f.getType();
                if (isSimpleType(t)) {
                    row.add(val);
                } else if (t.isArray() || Iterable.class.isAssignableFrom(t) || Map.class.isAssignableFrom(t)) {
                    row.add(stringify(val));
                } else {
                    row.add(stringify(val));
                }
            }
        }
        return row;
    }

    private static boolean isSimpleType(Class<?> t) {
        if (t.isPrimitive()) return true;
        List<Class<?>> simple = Arrays.asList(
            String.class,
            Boolean.class, Character.class,
            Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
            BigDecimal.class, BigInteger.class,
            Date.class, LocalDate.class, LocalDateTime.class, LocalTime.class, Instant.class,
            UUID.class
        );
        return simple.contains(t) || Number.class.isAssignableFrom(t);
    }

    private static String stringify(Object v) {
        try {
            return om.writeValueAsString(v);
        } catch (Throwable e) {
            return String.valueOf(v);
        }
    }
}
