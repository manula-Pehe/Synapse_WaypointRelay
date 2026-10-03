package com.synapse.waypoint.common.dto;

import java.util.List;

/** The list envelope used by every list endpoint: {@code { items, total }} (docs/api.md, Conventions). */
public record ListResponse<T>(List<T> items, int total) {

    public ListResponse {
        items = List.copyOf(items);
    }

    public static <T> ListResponse<T> of(List<T> items) {
        return new ListResponse<>(items, items.size());
    }
}
