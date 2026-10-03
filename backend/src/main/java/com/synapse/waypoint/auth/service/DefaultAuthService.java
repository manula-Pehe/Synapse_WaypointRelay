package com.synapse.waypoint.auth.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.auth.dto.LoginRequest;
import com.synapse.waypoint.auth.dto.LoginResponse;
import com.synapse.waypoint.auth.dto.UpdatePreferencesRequest;
import com.synapse.waypoint.auth.dto.UserResponse;
import com.synapse.waypoint.auth.entity.UserAccount;
import com.synapse.waypoint.auth.login.LoginMethod;
import com.synapse.waypoint.auth.repository.UserAccountRepository;
import com.synapse.waypoint.auth.token.AccessTokenIssuer;
import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.CurrentUser;

@Service
class DefaultAuthService implements AuthService {

    /** Same message for an unknown identifier and a wrong secret, so accounts can't be probed. */
    static final String SIGN_IN_FAILED = "Incorrect sign-in details.";

    private final List<LoginMethod> loginMethods;
    private final AccessTokenIssuer tokenIssuer;
    private final UserAccountRepository users;
    private final CurrentUser currentUser;

    DefaultAuthService(List<LoginMethod> loginMethods, AccessTokenIssuer tokenIssuer,
            UserAccountRepository users, CurrentUser currentUser) {
        this.loginMethods = List.copyOf(loginMethods);
        this.tokenIssuer = tokenIssuer;
        this.users = users;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        UserAccount user = loginMethods.stream()
                .filter(method -> method.supports(identifier))
                .findFirst()
                .flatMap(method -> method.authenticate(identifier, request.secret()))
                .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHORIZED, SIGN_IN_FAILED));
        return new LoginResponse(tokenIssuer.issue(user), UserMapper.toResponse(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        return UserMapper.toResponse(signedInUser());
    }

    @Override
    @Transactional
    public UserResponse updatePreferences(UpdatePreferencesRequest request) {
        UserAccount user = signedInUser();
        user.updatePreferences(request.language(), request.theme());
        return UserMapper.toResponse(user);
    }

    private UserAccount signedInUser() {
        String id = currentUser.id();
        return users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }
}
