package com.synapse.waypoint.core.order.dto;

import java.util.List;

/** An order with its history (D10, S3p). */
public record OrderDetailDto(OrderDto order, List<OrderEventDto> history) {

    public OrderDetailDto {
        history = List.copyOf(history);
    }
}
