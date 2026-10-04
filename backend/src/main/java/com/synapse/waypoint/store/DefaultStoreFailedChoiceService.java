package com.synapse.waypoint.store;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.reference.repository.OutletRepository;
import com.synapse.waypoint.driver.dto.DeliveryDto;
import com.synapse.waypoint.driver.entity.FailedDeliveryDecision;
import com.synapse.waypoint.driver.service.DeliveryQueryService;
import com.synapse.waypoint.driver.service.DispatchDecisionService;
import com.synapse.waypoint.notification.entity.NotificationSeverity;
import com.synapse.waypoint.notification.recipient.NotificationScope;
import com.synapse.waypoint.notification.service.NotificationService;

/** Applies the store's S3f answer through the driver module's own decision service. */
@Service
@Transactional
class DefaultStoreFailedChoiceService implements StoreFailedChoiceService {

    private static final String DELIVERY = "delivery";
    private static final String DISPATCH_LINK = "/dispatch";

    private final DeliveryQueryService deliveryQuery;
    private final DispatchDecisionService decisions;
    private final OrderService orders;
    private final StoreOrderAccess access;
    private final OutletRepository outlets;
    private final NotificationService notifications;

    DefaultStoreFailedChoiceService(DeliveryQueryService deliveryQuery, DispatchDecisionService decisions,
            OrderService orders, StoreOrderAccess access, OutletRepository outlets,
            NotificationService notifications) {
        this.deliveryQuery = deliveryQuery;
        this.decisions = decisions;
        this.orders = orders;
        this.access = access;
        this.outlets = outlets;
        this.notifications = notifications;
    }

    @Override
    public void choose(String deliveryId, FailedDeliveryDecision choice) {
        DeliveryDto delivery = deliveryQuery.deliveryById(deliveryId)
                .orElseThrow(() -> new NotFoundException(DELIVERY, deliveryId));
        OrderDto order = orders.get(delivery.orderId());
        if (!order.outletId().equals(access.outletId())) {
            throw new NotFoundException(DELIVERY, deliveryId);
        }
        requireOpenFailure(delivery);
        decisions.decideFailedDelivery(deliveryId, choice);
        notifyDispatchers(order, choice);
    }

    private void requireOpenFailure(DeliveryDto delivery) {
        if (delivery.outcome() != DeliveryOutcome.FAILED) {
            throw new DomainException(ErrorCode.INVALID_STATUS, "Only a failed delivery can be answered.");
        }
        if (delivery.isDecided()) {
            throw new DomainException(ErrorCode.DUPLICATE, "This failed delivery already has an answer.");
        }
    }

    private void notifyDispatchers(OrderDto order, FailedDeliveryDecision choice) {
        String depot = outlets.findById(order.outletId()).orElseThrow().getDepot();
        notifications.notifyRole(Role.DISPATCHER, NotificationScope.depot(depot), NotificationSeverity.INFO,
                "STORE_FAILED_CHOICE", "Store answered a failed delivery",
                "Store chose " + choice + " for failed " + order.ref(), DISPATCH_LINK);
    }
}
