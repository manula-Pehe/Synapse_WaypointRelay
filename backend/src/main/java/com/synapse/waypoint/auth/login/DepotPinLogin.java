package com.synapse.waypoint.auth.login;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.synapse.waypoint.auth.entity.UserAccount;
import com.synapse.waypoint.auth.repository.UserAccountRepository;
import com.synapse.waypoint.common.security.Role;

/**
 * Loaders on the shared dock tablet: depot name + personal PIN.
 * The PIN identifies which loader at that depot is signing in.
 */
@Component
class DepotPinLogin implements LoginMethod {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    DepotPinLogin(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean supports(String identifier) {
        return !IdentifierPatterns.isEmail(identifier) && !IdentifierPatterns.isStaffId(identifier);
    }

    @Override
    public Optional<UserAccount> authenticate(String identifier, String secret) {
        return users.findByRoleAndDepotIgnoreCaseAndActiveTrue(Role.LOADER, identifier).stream()
                .filter(loader -> passwordEncoder.matches(secret, loader.getSecretHash()))
                .findFirst();
    }
}
