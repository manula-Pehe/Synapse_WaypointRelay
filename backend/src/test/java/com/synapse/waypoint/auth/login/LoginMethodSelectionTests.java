package com.synapse.waypoint.auth.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.synapse.waypoint.auth.repository.UserAccountRepository;

/** Each identifier must be handled by exactly one login method. */
class LoginMethodSelectionTests {

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    private final LoginMethod email = new EmailPasswordLogin(users, encoder);
    private final LoginMethod staff = new StaffPinLogin(users, encoder);
    private final LoginMethod depot = new DepotPinLogin(users, encoder);

    @Test
    void shouldRouteEmailsToEmailPasswordOnly() {
        assertOnlySupportedBy(email, "store.manager@example.lk");
    }

    @Test
    void shouldRouteStaffIdsToStaffPinOnly() {
        assertOnlySupportedBy(staff, "DRV-0900");
        assertOnlySupportedBy(staff, "drv-0900");
    }

    @Test
    void shouldRouteDepotNamesToDepotPinOnly() {
        assertOnlySupportedBy(depot, "Peliyagoda");
        assertOnlySupportedBy(depot, "Kandy");
    }

    private void assertOnlySupportedBy(LoginMethod expected, String identifier) {
        for (LoginMethod method : new LoginMethod[] {email, staff, depot}) {
            assertThat(method.supports(identifier))
                    .as("%s supports %s", method.getClass().getSimpleName(), identifier)
                    .isEqualTo(method == expected);
        }
    }
}
