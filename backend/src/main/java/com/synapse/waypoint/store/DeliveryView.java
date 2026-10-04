package com.synapse.waypoint.store;

import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.planning.dto.StoreDeferralDto;

/** One order of a run as the store sees it (docs/api.md §7). Delivery and shortfall come from other modules. */
record DeliveryView(String orderId, String orderRef, OrderStatus status, ArrivalView arrival,
        StoreDeferralDto deferral, Object delivery, Object shortfall, ReceiptView receipt) {
}
