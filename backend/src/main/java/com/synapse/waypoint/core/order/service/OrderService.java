package com.synapse.waypoint.core.order.service;

import java.time.LocalDate;
import java.util.List;

import com.synapse.waypoint.core.order.dto.CreateOrderRequest;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderEventDto;
import com.synapse.waypoint.core.order.dto.OrderFilters;
import com.synapse.waypoint.core.order.entity.DeliveryOutcome;

/**
 * The only way any module changes an order's status (docs/api.md §11). Every change checks the
 * lifecycle, writes one history row and fails with {@code INVALID_STATUS} when not allowed.
 */
public interface OrderService {

    OrderDto get(String orderId);

    /**
     * The orders of one run date, optionally of one depot, by reference. A store manager always gets
     * only their own outlet's orders.
     */
    List<OrderDto> findByRun(LocalDate runDate, String depot, OrderFilters filters);

    OrderDto confirm(String orderId);

    /** Confirms at the cut-off without a user and marks the order as auto-confirmed. */
    OrderDto autoConfirm(String orderId);

    /** Only a PREPARED order can be edited. */
    OrderDto editUnits(String orderId, int units);

    OrderDto cancel(String orderId, String reason);

    OrderDto markPlanned(String orderId, String planId);

    OrderDto markMoved(String orderId, LocalDate newDate, String reason);

    OrderDto markLoaded(String orderId);

    OrderDto markOnTheWay(String orderId);

    /** Ends the delivery as DELIVERED, PARTIAL or FAILED; {@code units} is how many were delivered. */
    OrderDto recordOutcome(String orderId, DeliveryOutcome outcome, int units);

    /** A store's extra order: starts PREPARED, for the store's own outlet only. */
    OrderDto createStoreOrder(CreateOrderRequest request);

    /** An order the dispatcher took by phone: starts CONFIRMED but not yet checked by the store. */
    OrderDto createPhoneInOrder(CreateOrderRequest request);

    /** The unserved part of an order, as a new CONFIRMED order whose reference is the parent's plus "-R". */
    OrderDto createRemainder(String parentOrderId, int units, String reason);

    List<OrderEventDto> history(String orderId);
}
