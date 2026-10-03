package com.synapse.waypoint.common.security;

/** Names of the claims carried in a login token. Shared by the token issuer and {@link CurrentUser}. */
public final class TokenClaims {

    public static final String ROLE = "role";
    public static final String NAME = "name";
    public static final String OUTLET_ID = "outletId";
    public static final String DEPOT = "depot";
    public static final String VEHICLE_ID = "vehicleId";

    private TokenClaims() {
    }
}
