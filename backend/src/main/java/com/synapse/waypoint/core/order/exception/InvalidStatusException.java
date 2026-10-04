package com.synapse.waypoint.core.order.exception;

import java.util.Map;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.core.order.entity.OrderStatus;

/** An order change that its current status does not allow (409 {@code INVALID_STATUS}). */
public class InvalidStatusException extends DomainException {

    private InvalidStatusException(String message, Map<String, ?> details) {
        super(ErrorCode.INVALID_STATUS, message, details);
    }

    public static InvalidStatusException transition(OrderStatus from, OrderStatus to) {
        return new InvalidStatusException("An order that is " + from + " cannot become " + to + ".",
                Map.of("from", from.name(), "to", to.name()));
    }

    public static InvalidStatusException notEditable(OrderStatus status) {
        return new InvalidStatusException("Only a PREPARED order can be edited, this one is " + status + ".",
                Map.of("status", status.name()));
    }
}
