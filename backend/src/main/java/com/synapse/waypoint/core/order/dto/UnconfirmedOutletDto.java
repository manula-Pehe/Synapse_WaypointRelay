package com.synapse.waypoint.core.order.dto;

import java.util.List;

/**
 * One store that has orders still PREPARED (D1u). {@code phone} stays null until outlets store a
 * phone number.
 */
public record UnconfirmedOutletDto(String outletId, String outletName, String phone, List<OrderDto> orders) {

    public UnconfirmedOutletDto {
        orders = List.copyOf(orders);
    }
}
