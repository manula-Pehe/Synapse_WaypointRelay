package com.synapse.waypoint.auth.token;

import com.synapse.waypoint.auth.entity.UserAccount;

/** Creates the bearer token returned at sign-in. */
public interface AccessTokenIssuer {

    String issue(UserAccount user);
}
