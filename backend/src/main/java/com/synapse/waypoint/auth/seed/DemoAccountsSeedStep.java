package com.synapse.waypoint.auth.seed;

import java.time.Instant;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.synapse.waypoint.auth.entity.UserAccount;
import com.synapse.waypoint.auth.repository.UserAccountRepository;
import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.common.seed.DemoDay;
import com.synapse.waypoint.common.seed.SeedStep;
import com.synapse.waypoint.common.time.DemoClock;

/** Creates the four demo accounts (one per role). Runs after the outlets and vehicles they refer to exist. */
@Component
class DemoAccountsSeedStep implements SeedStep {

    static final int ORDER = 40;

    private static final String PASSWORD = "Relay@2026";

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final DemoClock clock;

    DemoAccountsSeedStep(UserAccountRepository users, PasswordEncoder encoder, DemoClock clock) {
        this.users = users;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public String name() {
        return "demo accounts";
    }

    @Override
    public void run() {
        Instant now = clock.now();
        users.saveAll(List.of(
                UserAccount.create("usr-dilani", "Dilani J.", Role.STORE_MANAGER, "dilani@waypoint.lk", null,
                        encoder.encode(PASSWORD), DemoDay.STORE_OUTLET_ID, null, null, now),
                UserAccount.create("usr-ruwan", "Ruwan P.", Role.DISPATCHER, "ruwan@waypoint.lk", null,
                        encoder.encode(PASSWORD), null, null, null, now),
                UserAccount.create("usr-kasun", "Kasun", Role.LOADER, null, null,
                        encoder.encode("1234"), null, DemoDay.LOADER_DEPOT, null, now),
                UserAccount.create("usr-nuwan", "Nuwan S.", Role.DRIVER, null, "DRV-0036",
                        encoder.encode("3636"), null, null, DemoDay.DRIVER_VEHICLE_ID, now)));
    }
}
