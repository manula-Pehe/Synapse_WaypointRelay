package com.synapse.waypoint.core.order.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.dto.ListResponse;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.CloseStatusDto;
import com.synapse.waypoint.core.order.dto.OrderDetailDto;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderFilters;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.service.OrderService;

/** Order list and detail - docs/api.md §4. */
@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService orderService;
    private final CloseOrdersService closeOrders;
    private final DemoClock clock;

    OrderController(OrderService orderService, CloseOrdersService closeOrders, DemoClock clock) {
        this.orderService = orderService;
        this.closeOrders = closeOrders;
        this.clock = clock;
    }

    /** {@code runDate} defaults to the current run date; a store manager only ever sees their own outlet. */
    @GetMapping
    ListResponse<OrderDto> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate,
            @RequestParam(required = false) String depot,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String outletId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) TemperatureRequirement temp) {
        LocalDate date = runDate != null ? runDate : clock.runDate();
        return ListResponse.of(orderService.findByRun(date, depot, new OrderFilters(status, outletId, brand, temp)));
    }

    /** Whether the orders of a run are closed, and when they close; open to every signed-in role. */
    @GetMapping("/close-status")
    CloseStatusDto closeStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate,
            @RequestParam String depot) {
        return closeOrders.status(runDate != null ? runDate : clock.runDate(), depot);
    }

    @GetMapping("/{id}")
    OrderDetailDto detail(@PathVariable String id) {
        return new OrderDetailDto(orderService.get(id), orderService.history(id));
    }
}
