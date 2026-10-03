package com.synapse.waypoint.auth.login;

import java.util.Optional;

import com.synapse.waypoint.auth.entity.UserAccount;

/**
 * One way of signing in. Each method recognises its own kind of identifier;
 * add a new way by adding a new implementation.
 */
public interface LoginMethod {

    boolean supports(String identifier);

    Optional<UserAccount> authenticate(String identifier, String secret);
}
