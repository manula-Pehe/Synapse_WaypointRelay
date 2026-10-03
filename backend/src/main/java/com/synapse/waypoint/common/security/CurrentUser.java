package com.synapse.waypoint.common.security;

import java.util.Optional;

/**
 * The signed-in user of the current request. Services use it for data-scope checks:
 * a store sees only its outlet, a driver only its vehicle, a loader only its depot.
 */
public interface CurrentUser {

    String id();

    Role role();

    Optional<String> outletId();

    Optional<String> depot();

    Optional<String> vehicleId();

    default boolean is(Role expected) {
        return role() == expected;
    }
}
