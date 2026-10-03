package com.synapse.waypoint.core.order.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderEventDto;
import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderEvent;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.repository.OrderEventRepository;
import com.synapse.waypoint.core.order.repository.OrderRepository;

@Service
@Transactional
class DefaultOrderService implements OrderService {

    private static final String EDITED = "EDITED";

    private final OrderRepository orders;
    private final OrderEventRepository events;
    private final OrderMapper mapper;
    private final OrderAccessPolicy access;
    private final OrderChangeGuard changeGuard;
    private final DemoClock clock;
    private final CurrentUser currentUser;

    DefaultOrderService(OrderRepository orders, OrderEventRepository events, OrderMapper mapper,
            OrderAccessPolicy access, OrderChangeGuard changeGuard, DemoClock clock, CurrentUser currentUser) {
        this.orders = orders;
        this.events = events;
        this.mapper = mapper;
        this.access = access;
        this.changeGuard = changeGuard;
        this.clock = clock;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto get(String orderId) {
        return mapper.toDto(load(orderId));
    }

    @Override
    public OrderDto confirm(String orderId) {
        Order order = load(orderId);
        changeGuard.requireOpenForChanges(order);
        OrderStatus from = order.getStatus();
        order.confirm(currentUser.idIfSignedIn().orElse(null), clock.now());
        return saveWithEvent(order, from, Map.of());
    }

    @Override
    public OrderDto editUnits(String orderId, int units) {
        requirePositive(units);
        Order order = load(orderId);
        changeGuard.requireOpenForChanges(order);
        int previousUnits = order.getUnits();
        order.editUnits(units, clock.now());
        return saveWithEvent(order, EDITED, order.getStatus(), Map.of("fromUnits", previousUnits, "toUnits", units));
    }

    @Override
    public OrderDto cancel(String orderId, String reason) {
        Order order = load(orderId);
        changeGuard.requireOpenForChanges(order);
        return transition(order, OrderStatus.CANCELLED, withReason(reason));
    }

    @Override
    public OrderDto markPlanned(String orderId, String planId) {
        if (planId == null || planId.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION, "A plan id is required.",
                    Map.of("planId", "must not be blank"));
        }
        return transition(load(orderId), OrderStatus.PLANNED, Map.of("planId", planId));
    }

    @Override
    public OrderDto markMoved(String orderId, LocalDate newDate, String reason) {
        Order order = load(orderId);
        if (!newDate.isAfter(order.getRunDate())) {
            throw new DomainException(ErrorCode.VALIDATION, "The new date must be after the current run date.",
                    Map.of("newDate", "must be after " + order.getRunDate()));
        }
        Map<String, Object> details = withReason(reason);
        details.put("fromDate", order.getRunDate().toString());
        details.put("toDate", newDate.toString());
        OrderStatus from = order.getStatus();
        order.moveTo(newDate, clock.now());
        return saveWithEvent(order, from, details);
    }

    @Override
    public OrderDto markLoaded(String orderId) {
        return transition(load(orderId), OrderStatus.LOADED, Map.of());
    }

    @Override
    public OrderDto markOnTheWay(String orderId) {
        return transition(load(orderId), OrderStatus.ON_THE_WAY, Map.of());
    }

    @Override
    public OrderDto recordOutcome(String orderId, DeliveryOutcome outcome, int units) {
        Order order = load(orderId);
        if (units < 0 || units > order.getUnits()) {
            throw new DomainException(ErrorCode.VALIDATION, "Delivered units must be between 0 and the ordered units.",
                    Map.of("units", "must be between 0 and " + order.getUnits()));
        }
        return transition(order, outcome.status(), Map.of("units", units));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderEventDto> history(String orderId) {
        load(orderId);
        return events.findByOrderIdOrderByAtAscIdAsc(orderId).stream().map(mapper::toDto).toList();
    }

    private OrderDto transition(Order order, OrderStatus target, Map<String, Object> details) {
        OrderStatus from = order.getStatus();
        order.changeStatus(target, clock.now());
        return saveWithEvent(order, from, details);
    }

    private OrderDto saveWithEvent(Order order, OrderStatus from, Map<String, Object> details) {
        return saveWithEvent(order, order.getStatus().name(), from, details);
    }

    private OrderDto saveWithEvent(Order order, String type, OrderStatus from, Map<String, Object> details) {
        Order saved = orders.saveAndFlush(order);
        Instant at = saved.getUpdatedAt();
        events.save(new OrderEvent(saved.getId(), at, currentUser.idIfSignedIn().orElse(null), type, from,
                saved.getStatus(), details));
        return mapper.toDto(saved);
    }

    private Order load(String orderId) {
        Order order = orders.findById(orderId).orElseThrow(() -> new NotFoundException("Order", orderId));
        return access.requireVisible(order);
    }

    private static Map<String, Object> withReason(String reason) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (reason != null && !reason.isBlank()) {
            details.put("reason", reason.strip());
        }
        return details;
    }

    private static void requirePositive(int units) {
        if (units <= 0) {
            throw new DomainException(ErrorCode.VALIDATION, "Units must be more than zero.",
                    Map.of("units", "must be more than zero"));
        }
    }
}
