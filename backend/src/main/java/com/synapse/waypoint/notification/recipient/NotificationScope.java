package com.synapse.waypoint.notification.recipient;

import java.util.Optional;

/**
 * Narrows which users of a role receive a notification. A missing field means "the whole role".
 * Store and driver notifications should always name the outlet or vehicle.
 */
public record NotificationScope(Optional<String> outletId, Optional<String> depot, Optional<String> vehicleId) {

    public static NotificationScope none() {
        return new NotificationScope(Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static NotificationScope outlet(String outletId) {
        return new NotificationScope(Optional.of(outletId), Optional.empty(), Optional.empty());
    }

    public static NotificationScope depot(String depot) {
        return new NotificationScope(Optional.empty(), Optional.of(depot), Optional.empty());
    }

    public static NotificationScope vehicle(String vehicleId) {
        return new NotificationScope(Optional.empty(), Optional.empty(), Optional.of(vehicleId));
    }
}
