package com.synapse.waypoint.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.reference.support.ReferenceFixtures;
import com.synapse.waypoint.notification.entity.NotificationSeverity;
import com.synapse.waypoint.notification.recipient.NotificationScope;
import com.synapse.waypoint.notification.service.NotificationService;

/** Who receives a notification, by role and scope. Users, outlets and vehicles are invented. */
@SpringBootTest
@Transactional
class NotificationFanOutTests {

    private static final String TYPE = "FANOUT_TEST";

    @Autowired NotificationService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired DemoClock clock;
    @Autowired EntityManager entityManager;

    @BeforeEach
    void createUsers() {
        OrderFixtures.insertOutlet(jdbc, "OUT981", "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, "OUT982", "Fresh", "Testdistrict");
        ReferenceFixtures.insertVehicle(jdbc, "VEH981", "ambient", "Depot-A");
        ReferenceFixtures.insertVehicle(jdbc, "VEH982", "ambient", "Depot-A");
        insertUser("usr-n-store-a1", "STORE_MANAGER", "OUT981", null, null, true);
        insertUser("usr-n-store-a2", "STORE_MANAGER", "OUT981", null, null, true);
        insertUser("usr-n-store-a-off", "STORE_MANAGER", "OUT981", null, null, false);
        insertUser("usr-n-store-b", "STORE_MANAGER", "OUT982", null, null, true);
        insertUser("usr-n-disp-all", "DISPATCHER", null, null, null, true);
        insertUser("usr-n-disp-a", "DISPATCHER", null, "Depot-A", null, true);
        insertUser("usr-n-disp-b", "DISPATCHER", null, "Depot-B", null, true);
        insertUser("usr-n-load-a", "LOADER", null, "Depot-A", null, true);
        insertUser("usr-n-load-b", "LOADER", null, "Depot-B", null, true);
        insertUser("usr-n-drv-1", "DRIVER", null, null, "VEH981", true);
        insertUser("usr-n-drv-2", "DRIVER", null, null, "VEH982", true);
    }

    @Test
    void shouldReachOnlyTheManagersOfTheScopedOutlet() {
        notifyRole(Role.STORE_MANAGER, NotificationScope.outlet("OUT981"));

        assertThat(recipients()).containsExactlyInAnyOrder("usr-n-store-a1", "usr-n-store-a2");
    }

    @Test
    void shouldReachEveryUserOfTheRoleWhenTheScopeIsEmpty() {
        notifyRole(Role.STORE_MANAGER, NotificationScope.none());

        assertThat(recipients()).contains("usr-n-store-a1", "usr-n-store-a2", "usr-n-store-b")
                .doesNotContain("usr-n-store-a-off");
    }

    @Test
    void shouldSkipInactiveUsers() {
        notifyRole(Role.STORE_MANAGER, NotificationScope.outlet("OUT981"));

        assertThat(recipients()).doesNotContain("usr-n-store-a-off");
    }

    @Test
    void shouldReachDispatchersOfAllDepotsWhenNoDepotIsGiven() {
        notifyRole(Role.DISPATCHER, NotificationScope.none());

        assertThat(recipients()).contains("usr-n-disp-all", "usr-n-disp-a", "usr-n-disp-b");
    }

    @Test
    void shouldReachDispatchersOfThatDepotAndThoseWorkingAcrossAllDepots() {
        notifyRole(Role.DISPATCHER, NotificationScope.depot("Depot-A"));

        assertThat(recipients()).contains("usr-n-disp-all", "usr-n-disp-a").doesNotContain("usr-n-disp-b");
    }

    @Test
    void shouldReachOnlyLoadersOfTheScopedDepot() {
        notifyRole(Role.LOADER, NotificationScope.depot("Depot-B"));

        assertThat(recipients()).contains("usr-n-load-b").doesNotContain("usr-n-load-a");
    }

    @Test
    void shouldReachOnlyTheDriverOfTheScopedVehicle() {
        notifyRole(Role.DRIVER, NotificationScope.vehicle("VEH982"));

        assertThat(recipients()).containsExactly("usr-n-drv-2");
    }

    @Test
    void shouldStoreOneRowPerRecipientWithDemoClockTime() {
        Instant before = clock.now();

        notifyRole(Role.STORE_MANAGER, NotificationScope.outlet("OUT981"));

        entityManager.flush();
        List<Instant> times = jdbc.queryForList("SELECT created_at FROM notifications WHERE type = ?",
                java.sql.Timestamp.class, TYPE).stream().map(java.sql.Timestamp::toInstant).toList();
        assertThat(times).hasSize(2).allSatisfy(at -> assertThat(at).isBetween(before, clock.now()));
    }

    @Test
    void shouldNotifyASingleActiveUser() {
        service.notifyUser("usr-n-store-b", NotificationSeverity.WARNING, TYPE, "Title", "Body", "/store");

        assertThat(recipients()).containsExactly("usr-n-store-b");
    }

    @Test
    void shouldRefuseToNotifyAnUnknownOrInactiveUser() {
        assertThatThrownBy(() -> service.notifyUser("usr-n-nobody", NotificationSeverity.INFO, TYPE, "T", "B", null))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.notifyUser("usr-n-store-a-off", NotificationSeverity.INFO, TYPE, "T", "B",
                null)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldRejectANotificationWithoutTitle() {
        assertThatThrownBy(() -> service.notifyUser("usr-n-store-b", NotificationSeverity.INFO, TYPE, " ", "B", null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldDeliverCriticalNotificationsLikeAnyOther() {
        service.notifyRole(Role.DRIVER, NotificationScope.vehicle("VEH981"), NotificationSeverity.CRITICAL, TYPE,
                "Title", "Body", null);

        assertThat(recipients()).containsExactly("usr-n-drv-1");
    }

    private void notifyRole(Role role, NotificationScope scope) {
        service.notifyRole(role, scope, NotificationSeverity.INFO, TYPE, "Title", "Body", "/link");
    }

    private List<String> recipients() {
        entityManager.flush();
        return jdbc.queryForList("SELECT user_id FROM notifications WHERE type = ?", String.class, TYPE);
    }

    private void insertUser(String id, String role, String outletId, String depot, String vehicleId,
            boolean active) {
        jdbc.update("""
                INSERT INTO users (id, name, role, email, secret_hash, outlet_id, depot, vehicle_id, active)
                VALUES (?, ?, ?, ?, 'x', ?, ?, ?, ?)""", id, "Test " + role, role, id + "@example.lk", outletId, depot,
                vehicleId, active);
    }
}
