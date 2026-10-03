package com.synapse.waypoint.auth.login;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.synapse.waypoint.auth.entity.UserAccount;
import com.synapse.waypoint.auth.repository.UserAccountRepository;

/** Drivers: staff ID + PIN on their own phone. */
@Component
class StaffPinLogin implements LoginMethod {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    StaffPinLogin(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean supports(String identifier) {
        return IdentifierPatterns.isStaffId(identifier);
    }

    @Override
    public Optional<UserAccount> authenticate(String identifier, String secret) {
        return users.findByStaffIdIgnoreCaseAndActiveTrue(identifier)
                .filter(user -> passwordEncoder.matches(secret, user.getSecretHash()));
    }
}
