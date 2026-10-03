package com.synapse.waypoint.auth.login;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.synapse.waypoint.auth.entity.UserAccount;
import com.synapse.waypoint.auth.repository.UserAccountRepository;

/** Store managers and dispatchers: email + password. */
@Component
class EmailPasswordLogin implements LoginMethod {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    EmailPasswordLogin(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean supports(String identifier) {
        return IdentifierPatterns.isEmail(identifier);
    }

    @Override
    public Optional<UserAccount> authenticate(String identifier, String secret) {
        return users.findByEmailIgnoreCaseAndActiveTrue(identifier)
                .filter(user -> passwordEncoder.matches(secret, user.getSecretHash()));
    }
}
