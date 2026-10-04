package com.synapse.waypoint.core.order.controller;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.dto.ListResponse;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.CloseOrdersRequest;
import com.synapse.waypoint.core.order.dto.CloseResultDto;
import com.synapse.waypoint.core.order.dto.CreateOrderRequest;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.UnconfirmedOutletDto;
import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.order.service.UnconfirmedOrderService;

/** Dispatcher order endpoints - docs/api.md §4 (D1, D1b, D1u). Dispatcher-only by path rule. */
@RestController
@RequestMapping("/api/dispatch/orders")
class DispatchOrderController {

    private final OrderService orderService;
    private final UnconfirmedOrderService unconfirmedOrders;
    private final CloseOrdersService closeOrders;
    private final DemoClock clock;

    DispatchOrderController(OrderService orderService, UnconfirmedOrderService unconfirmedOrders,
            CloseOrdersService closeOrders, DemoClock clock) {
        this.orderService = orderService;
        this.unconfirmedOrders = unconfirmedOrders;
        this.closeOrders = closeOrders;
        this.clock = clock;
    }

    /** The D1 "Close orders" button. */
    @PostMapping("/close")
    CloseResultDto close(@Valid @RequestBody CloseOrdersRequest request) {
        return closeOrders.close(request.runDate(), request.depot());
    }

    @PostMapping("/phone-in")
    @ResponseStatus(HttpStatus.CREATED)
    OrderDto phoneIn(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createPhoneInOrder(request);
    }

    /** {@code runDate} defaults to the current run date. */
    @GetMapping("/unconfirmed")
    ListResponse<UnconfirmedOutletDto> unconfirmed(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate,
            @RequestParam(required = false) String depot) {
        return ListResponse.of(unconfirmedOrders.unconfirmed(runDate != null ? runDate : clock.runDate(), depot));
    }
}
