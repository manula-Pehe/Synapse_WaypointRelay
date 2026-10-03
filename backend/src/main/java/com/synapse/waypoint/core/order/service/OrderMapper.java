package com.synapse.waypoint.core.order.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.time.ApiTimestamp;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderEventDto;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderEvent;
import com.synapse.waypoint.core.reference.entity.Outlet;
import com.synapse.waypoint.core.reference.repository.OutletRepository;

/** Entity to DTO. Outlet names are "OUT001 · District" until outlets have a name column. */
@Component
class OrderMapper {

    private static final String OUTLET_NAME_FORMAT = "%s · %s";

    private final OutletRepository outlets;

    OrderMapper(OutletRepository outlets) {
        this.outlets = outlets;
    }

    OrderDto toDto(Order order) {
        return toDtos(List.of(order)).get(0);
    }

    List<OrderDto> toDtos(Collection<Order> orders) {
        Map<String, Outlet> byId = outlets.findAllById(orders.stream().map(Order::getOutletId).distinct().toList())
                .stream().collect(Collectors.toMap(Outlet::getId, Function.identity()));
        return orders.stream().map(order -> toDto(order, byId.get(order.getOutletId()))).toList();
    }

    OrderEventDto toDto(OrderEvent event) {
        return new OrderEventDto(ApiTimestamp.of(event.getAt()), event.getActorUserId(), event.getType(), event.getFromStatus(),
                event.getToStatus(), event.getDetails());
    }

    private static OrderDto toDto(Order order, Outlet outlet) {
        return new OrderDto(order.getId(), order.getRef(), order.getOutletId(),
                OUTLET_NAME_FORMAT.formatted(order.getOutletId(), outlet.getDistrict()), order.getBrand(),
                order.getTemperatureRequirement(), order.getUnits(), order.getWeightKg(), order.getVolumeM3(),
                order.getRunDate(), order.getStatus(), order.getSource(), order.isAutoConfirm(),
                order.getDaysSinceLastServed(), order.isDeferredYesterday(), order.getParentOrderId(),
                order.isStoreChecked(), ApiTimestamp.of(order.getConfirmedAt()),
                ApiTimestamp.of(order.getUpdatedAt()));
    }
}
