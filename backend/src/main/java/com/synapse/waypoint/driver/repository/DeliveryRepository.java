package com.synapse.waypoint.driver.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.driver.entity.Delivery;

public interface DeliveryRepository extends JpaRepository<Delivery, String> {

    /** The live delivery at a stop - an undone one does not count. */
    Optional<Delivery> findByStopIdAndUndoneAtIsNull(String stopId);

    Optional<Delivery> findByOrderIdAndUndoneAtIsNull(String orderId);

    boolean existsByClientId(String clientId);

    java.util.List<Delivery> findByVehicleIdOrderByCompletedAtDesc(String vehicleId);

    /** The deliveries that did not go through, for the dispatcher's decision. */
    java.util.List<Delivery> findByOutcomeNotAndUndoneAtIsNullOrderByCompletedAtDesc(
            com.synapse.waypoint.core.order.entity.DeliveryOutcome outcome);
}