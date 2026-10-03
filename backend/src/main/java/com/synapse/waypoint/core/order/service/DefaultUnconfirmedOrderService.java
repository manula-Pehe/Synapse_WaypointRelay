package com.synapse.waypoint.core.order.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderFilters;
import com.synapse.waypoint.core.order.dto.UnconfirmedOutletDto;
import com.synapse.waypoint.core.order.entity.OrderStatus;

@Service
@Transactional(readOnly = true)
class DefaultUnconfirmedOrderService implements UnconfirmedOrderService {

    private static final OrderFilters PREPARED_ONLY = new OrderFilters(OrderStatus.PREPARED, null, null, null);

    private final OrderService orderService;

    DefaultUnconfirmedOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public List<UnconfirmedOutletDto> unconfirmed(LocalDate runDate, String depot) {
        Map<String, List<OrderDto>> byOutlet = orderService.findByRun(runDate, depot, PREPARED_ONLY).stream()
                .collect(Collectors.groupingBy(OrderDto::outletId, TreeMap::new, Collectors.toList()));
        return byOutlet.values().stream().map(DefaultUnconfirmedOrderService::toOutletGroup).toList();
    }

    private static UnconfirmedOutletDto toOutletGroup(List<OrderDto> orders) {
        OrderDto first = orders.get(0);
        return new UnconfirmedOutletDto(first.outletId(), first.outletName(), null, orders);
    }
}
