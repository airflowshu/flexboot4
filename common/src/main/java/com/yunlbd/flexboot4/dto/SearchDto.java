package com.yunlbd.flexboot4.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SearchDto implements Serializable {
    private Integer pageNumber = 1;
    private Integer pageSize = 10;
    private List<OrderItem> orders;
    private List<SearchItem> items;
    private String logic = "AND";

    @Data
    public static class SearchItem implements Serializable {
        private String field;
        private String op;
        private Object val;
        private String logic = "AND";
        private List<SearchItem> children;
    }

    @Data
    public static class OrderItem implements Serializable {
        private String column;
        private boolean asc = true;
    }

    public static SearchItem item(String field, String op, Object val) {
        SearchItem it = new SearchItem();
        it.setField(field);
        it.setOp(op);
        it.setVal(val);
        return it;
    }
}

