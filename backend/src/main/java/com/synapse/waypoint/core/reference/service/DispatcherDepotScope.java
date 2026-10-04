package com.synapse.waypoint.core.reference.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.core.reference.repository.OutletRepository;

/**
 * Which depot a dispatcher may work on: a depot name nobody uses is a validation error (400); a
 * dispatcher with a depot sees only that depot (any other real depot is 404); a dispatcher without
 * one covers all depots.
 */
@Component
public class DispatcherDepotScope {

    private static final String DEPOT = "Depot";

    private final CurrentUser currentUser;
    private final OutletRepository outlets;

    public DispatcherDepotScope(CurrentUser currentUser, OutletRepository outlets) {
        this.currentUser = currentUser;
        this.outlets = outlets;
    }

    /** The depot as the outlets spell it, or the dispatcher's own depot when none is requested. */
    public String resolve(String requestedDepot) {
        String wanted = requestedDepot == null || requestedDepot.isBlank()
                ? currentUser.depot().orElseThrow(DispatcherDepotScope::depotRequired)
                : requestedDepot.strip();
        String depot = outlets.findDepotName(wanted).orElseThrow(DispatcherDepotScope::unknownDepot);
        requireInScope(depot);
        return depot;
    }

    /** Throws NotFound when the dispatcher has a depot and it is not the given one. */
    public void requireInScope(String depot) {
        currentUser.depot().filter(own -> !own.equalsIgnoreCase(depot))
                .ifPresent(own -> {
                    throw new NotFoundException(DEPOT, depot);
                });
    }

    private static DomainException unknownDepot() {
        return new DomainException(ErrorCode.VALIDATION, "Unknown depot.",
                Map.of("depot", "no outlet belongs to this depot"));
    }

    private static DomainException depotRequired() {
        return new DomainException(ErrorCode.VALIDATION, "Choose a depot.",
                Map.of("depot", "required when your account covers every depot"));
    }
}
