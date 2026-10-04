package com.synapse.waypoint.store;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.driver.dto.DeliveryDto;
import com.synapse.waypoint.driver.dto.DriverStatusDto;
import com.synapse.waypoint.driver.service.DeliveryQueryService;
import com.synapse.waypoint.loader.LoadingQueryService;

/** What the driver and loader modules know about one order, reshaped for the store's delivery list. */
@Component
class StoreDeliveryFacts {

    private static final String FILE_URL_PREFIX = "/api/files/";

    private final DeliveryQueryService driver;
    private final LoadingQueryService loader;

    StoreDeliveryFacts(DeliveryQueryService driver, LoadingQueryService loader) {
        this.driver = driver;
        this.loader = loader;
    }

    Optional<DeliveryProofView> delivery(String orderId) {
        return driver.deliveryForOrder(orderId).map(StoreDeliveryFacts::toProof);
    }

    Optional<ShortfallView> shortfall(String orderId) {
        return loader.shortfallForOrder(orderId)
                .map(result -> new ShortfallView(result.missingUnits(), result.reason(), result.remainderOrderRef()));
    }

    /** Only a truck that is out on the road has a phone worth reporting on. */
    Optional<DriverStatusView> driverStatus(OrderStatus status, ArrivalView arrival) {
        if (status != OrderStatus.ON_THE_WAY || arrival == null) {
            return Optional.empty();
        }
        DriverStatusDto dto = driver.driverStatus(arrival.vehicleId());
        return Optional.of(new DriverStatusView(dto.offline(), dto.lastSyncAt()));
    }

    private static DeliveryProofView toProof(DeliveryDto delivery) {
        return new DeliveryProofView(delivery.outcome(), delivery.units(), fileUrl(delivery.photoFileId()),
                fileUrl(delivery.signatureFileId()), delivery.receivedBy(), delivery.completedAt());
    }

    private static String fileUrl(String fileId) {
        return fileId == null ? null : FILE_URL_PREFIX + fileId;
    }
}
