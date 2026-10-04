package com.synapse.waypoint.driver.service;

import java.time.LocalDate;
import java.util.List;

import com.synapse.waypoint.driver.dto.ConflictDto;
import com.synapse.waypoint.driver.dto.DeliveryDto;
import com.synapse.waypoint.driver.entity.ConflictResolution;
import com.synapse.waypoint.driver.entity.FailedDeliveryDecision;

/**
 * The two decisions a dispatcher makes on what drivers did (docs/api.md §8, F8 and F9).
 *
 * They live together because both are the same act - settling one driver's offline work - and both
 * write a decision onto the record the driver left, so the driver sees the outcome on their own
 * screen without asking anyone.
 */
public interface DispatchDecisionService {

    /** D8 - the clashes waiting on a decision, newest first. */
    List<ConflictDto> openConflicts(LocalDate runDate);

    /**
     * D8 - settles a clash.
     *
     * {@link ConflictResolution#KEEP_FIELD} leaves the witnessed delivery standing, which is the
     * default because physical facts win: the goods were handed over and no board edit undoes that.
     */
    ConflictDto resolveConflict(String conflictId, ConflictResolution resolution);

    /** D6f - the deliveries that did not go through and still need a decision. */
    List<DeliveryDto> failedDeliveries(LocalDate runDate);

    /** D6f - what happens next for a failed delivery. */
    DeliveryDto decideFailedDelivery(String deliveryId, FailedDeliveryDecision decision);
}