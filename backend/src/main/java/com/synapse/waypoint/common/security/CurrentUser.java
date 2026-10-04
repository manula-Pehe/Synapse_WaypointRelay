package com.synapse.waypoint.common.security;

import java.util.Optional;

/**
 * The signed-in user of the current request. Services use it for data-scope checks:
 * a store sees only its outlet, a driver only its vehicle, a loader only its depot.
 */
public interface CurrentUser {

    String id();

    /** The user's id, or empty when nobody is signed in (timed jobs and other system work). */
    Optional<String> idIfSignedIn();

    Role role();

    Optional<String> outletId();

    Optional<String> depot();

    Optional<String> vehicleId();

    default boolean is(Role expected) {
        return role() == expected;
    }
}
