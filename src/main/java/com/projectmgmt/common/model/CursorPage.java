package com.projectmgmt.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CursorPage<T>(
        List<T> items,
        String nextCursor,
        String prevCursor,
        boolean hasMore,
        long totalCount
) {
    public static <T> CursorPage<T> of(List<T> items, String nextCursor, boolean hasMore, long totalCount) {
        return CursorPage.<T>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .totalCount(totalCount)
                .build();
    }

    public static <T> CursorPage<T> empty() {
        return CursorPage.<T>builder()
                .items(List.of())
                .hasMore(false)
                .totalCount(0)
                .build();
    }
}
