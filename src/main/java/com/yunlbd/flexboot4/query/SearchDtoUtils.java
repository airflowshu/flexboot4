package com.yunlbd.flexboot4.query;

import com.yunlbd.flexboot4.dto.SearchDto;

import java.util.List;

public final class SearchDtoUtils {

    private SearchDtoUtils() {}

    public static SearchDto create(Integer pageNumber,
                                   Integer pageSize,
                                   String logic,
                                   List<SearchDto.SearchItem> items,
                                   List<SearchDto.OrderItem> orders) {
        SearchDto dto = new SearchDto();
        if (pageNumber != null) {
            dto.setPageNumber(pageNumber);
        }
        if (pageSize != null) {
            dto.setPageSize(pageSize);
        }
        if (logic != null && !logic.isBlank()) {
            dto.setLogic(logic);
        }
        dto.setItems(items);
        dto.setOrders(orders);
        return dto;
    }

    public static boolean hasRelationPaths(SearchDto dto) {
        if (dto == null) return false;
        if (dto.getItems() != null) {
            for (SearchDto.SearchItem it : dto.getItems()) {
                if (containsRelationPath(it)) return true;
            }
        }
        if (dto.getOrders() != null) {
            for (SearchDto.OrderItem od : dto.getOrders()) {
                if (od.getColumn() != null && od.getColumn().contains(".")) return true;
            }
        }
        return false;
    }

    private static boolean containsRelationPath(SearchDto.SearchItem it) {
        if (it.getField() != null && it.getField().contains(".")) return true;
        if (it.getChildren() != null) {
            for (SearchDto.SearchItem c : it.getChildren()) {
                if (containsRelationPath(c)) return true;
            }
        }
        return false;
    }
}
