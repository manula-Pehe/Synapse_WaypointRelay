package com.synapse.waypoint.driver.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.synapse.waypoint.driver.dto.ConflictDto;
import com.synapse.waypoint.driver.dto.DeliveryDto;
import com.synapse.waypoint.driver.dto.DriverStatusDto;
import com.synapse.waypoint.driver.dto.VehicleProblemDto;

/**
 * Reads of what drivers did DeliveryQueryService.
 Nothing outside this module needs to know how a delivery is stored.
 */
public interface DeliveryQueryService {

    /** The live delivery for an order - an undone one  does not count. */
    Optional<DeliveryDto> deliveryForOrder(String orderId);

    /** One delivery by id, whatever its outcome; an undone delivery does not count. */
    Optional<DeliveryDto> deliveryById(String deliveryId);

    /**
     * How a vehicle's run is going, including when its phone last reached the server. A driver with
     * no signal shows as offline rather than as a stale position.
     */
    DriverStatusDto driverStatus(String vehicleId);

    /** The deliveries that did not go through, for the dispatcher's decision. */
    List<DeliveryDto> failedDeliveries(LocalDate runDate);

    /** The clashes waiting on a dispatcher. */
    List<ConflictDto> openConflicts(LocalDate runDate);

    /** The problems reported from the cab, newest first. */
    List<VehicleProblemDto> vehicleProblems(LocalDate runDate);
}