package com.synapse.waypoint.core.order.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.core.order.dto.CreateOrderRequest;
import com.synapse.waypoint.core.order.entity.NewOrder;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.reference.entity.Outlet;
import com.synapse.waypoint.core.reference.repository.OutletRepository;

/** Builds the specification of a new order: id, reference, brand and estimated size. */
@Component
class OrderFactory {

    private static final String REF_PREFIX = "ORD-";
    private static final int REF_LENGTH = 8;
    private static final String REMAINDER_SUFFIX = "-R";
    private static final int FIRST_NUMBERED_REMAINDER = 2;
    private static final int DEFAULT_DAYS_SINCE_LAST_SERVED = 1;

    private final OutletRepository outlets;
    private final OrderRepository orders;
    private final OrderSizeEstimator sizeEstimator;

    OrderFactory(OutletRepository outlets, OrderRepository orders, OrderSizeEstimator sizeEstimator) {
        this.outlets = outlets;
        this.orders = orders;
        this.sizeEstimator = sizeEstimator;
    }

    NewOrder storeOrder(CreateOrderRequest request) {
        return fromRequest(request, OrderStatus.PREPARED, OrderSource.STORE, true);
    }

    NewOrder phoneInOrder(CreateOrderRequest request) {
        return fromRequest(request, OrderStatus.CONFIRMED, OrderSource.PHONE_IN, false);
    }

    /** The unserved part of {@code parent}: same outlet, date and kind, size in proportion to the units. */
    NewOrder remainder(Order parent, int units) {
        OrderSize size = sizeEstimator.proportionalTo(parent, units);
        return new NewOrder(UUID.randomUUID().toString(), nextRemainderRef(parent.getRef()), parent.getOutletId(),
                parent.getBrand(), parent.getTemperatureRequirement(), units, size.weightKg(), size.volumeM3(),
                parent.getRunDate(), OrderStatus.CONFIRMED, OrderSource.REMAINDER, false,
                parent.getDaysSinceLastServed(), parent.isDeferredYesterday(), parent.getId(), true);
    }

    private NewOrder fromRequest(CreateOrderRequest request, OrderStatus status, OrderSource source,
            boolean storeChecked) {
        Outlet outlet = outlets.findById(request.outletId())
                .orElseThrow(() -> new NotFoundException("Outlet", request.outletId()));
        OrderSize size = sizeEstimator.estimate(outlet.getId(), outlet.getBrand(), request.temp(), request.units());
        return new NewOrder(UUID.randomUUID().toString(), newRef(), outlet.getId(), outlet.getBrand(),
                request.temp(), request.units(), size.weightKg(), size.volumeM3(), request.runDate(), status, source,
                false, DEFAULT_DAYS_SINCE_LAST_SERVED, false, null, storeChecked);
    }

    private String newRef() {
        return REF_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, REF_LENGTH).toUpperCase();
    }

    private String nextRemainderRef(String parentRef) {
        String ref = parentRef + REMAINDER_SUFFIX;
        for (int number = FIRST_NUMBERED_REMAINDER; orders.existsByRef(ref); number++) {
            ref = parentRef + REMAINDER_SUFFIX + number;
        }
        return ref;
    }
}
