package com.synapse.waypoint.planning.support;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.reference.support.ReferenceFixtures;

/**
 * An invented depot day for planning tests: one fridge vehicle, two outlets with a normal chilled order,
 * one outlet whose order is too heavy for any vehicle (so it is deferred), and the users who are told
 * about the plan. Orders are CONFIRMED; the caller closes the orders.
 */
public final class PlanScenario {

    public static final String DEPOT = "Plandepot";
    public static final String OTHER_DEPOT = "Otherdepot";
    public static final LocalDate RUN_DATE = LocalDate.parse("2030-01-10");
    public static final LocalDate NEXT_DAY = RUN_DATE.plusDays(1);
    public static final String PASSWORD = "Test-Password-1";
    public static final String PIN = "1234";

    public static final String STORE_1 = "P-OUT1";
    public static final String STORE_2 = "P-OUT2";
    public static final String HEAVY_STORE = "P-OUT3";
    public static final String IDLE_STORE = "P-OUT4";
    public static final String USED_VEHICLE = "P-VEH1";
    public static final String IDLE_VEHICLE = "P-VEH2";

    public static final String DISPATCHER = "usr-p-dispatch";
    public static final String DEPOT_DISPATCHER = "usr-p-dispatch-d";
    public static final String OTHER_DEPOT_DISPATCHER = "usr-p-dispatch-o";
    public static final String LOADER = "usr-p-loader";
    public static final String OTHER_DEPOT_LOADER = "usr-p-loader-o";
    public static final String USED_DRIVER = "usr-p-driver-1";
    public static final String IDLE_DRIVER = "usr-p-driver-2";

    private PlanScenario() {
    }

    public static void insert(JdbcTemplate jdbc, PasswordEncoder encoder) {
        insertReferenceData(jdbc);
        insertOrders(jdbc);
        insertUsers(jdbc, encoder);
    }

    public static String storeUser(String outletId) {
        return "usr-p-store-" + outletId;
    }

    public static String orderId(String ref) {
        return "ord-" + ref;
    }

    private static void insertReferenceData(JdbcTemplate jdbc) {
        for (String outlet : new String[] { STORE_1, STORE_2, HEAVY_STORE, IDLE_STORE }) {
            OrderFixtures.insertOutlet(jdbc, outlet, "Fresh", "Northvale", DEPOT);
        }
        OrderFixtures.insertOutlet(jdbc, "P-OUT9", "Fresh", "Southvale", OTHER_DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, USED_VEHICLE, "reefer", DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, IDLE_VEHICLE, "reefer", DEPOT);
        ReferenceFixtures.insertAvailability(jdbc, IDLE_VEHICLE, RUN_DATE, "IN_WORKSHOP", "Service");
        ReferenceFixtures.insertTravel(jdbc, "Northvale", DEPOT);
        ReferenceFixtures.upsertServiceAllowance(jdbc, "Fresh", "rear_dock", 10);
    }

    private static void insertOrders(JdbcTemplate jdbc) {
        insertOrder(jdbc, "PL-1", STORE_1, 100);
        insertOrder(jdbc, "PL-2", STORE_2, 120);
        insertOrder(jdbc, "PL-3", HEAVY_STORE, 5000);
    }

    private static void insertOrder(JdbcTemplate jdbc, String ref, String outletId, int weightKg) {
        jdbc.update("""
                INSERT INTO orders (id, ref, outlet_id, brand, temp_requirement, units, weight_kg, volume_m3,
                                    run_date, status, source)
                VALUES (?, ?, ?, 'Fresh', 'CHILLED', 10, ?, 1, ?, 'CONFIRMED', 'SEED')""",
                orderId(ref), ref, outletId, weightKg, RUN_DATE);
    }

    private static void insertUsers(JdbcTemplate jdbc, PasswordEncoder encoder) {
        String hash = encoder.encode(PASSWORD);
        String pinHash = encoder.encode(PIN);
        insertUser(jdbc, DISPATCHER, "DISPATCHER", "dispatch.p@example.lk", null, hash, null, null, null);
        insertUser(jdbc, DEPOT_DISPATCHER, "DISPATCHER", "depot.dispatch.p@example.lk", null, hash, null, DEPOT, null);
        insertUser(jdbc, OTHER_DEPOT_DISPATCHER, "DISPATCHER", "other.dispatch.p@example.lk", null, hash, null,
                OTHER_DEPOT, null);
        for (String outlet : new String[] { STORE_1, STORE_2, HEAVY_STORE, IDLE_STORE }) {
            insertUser(jdbc, storeUser(outlet), "STORE_MANAGER", outlet + "@example.lk", null, hash, outlet, null,
                    null);
        }
        insertUser(jdbc, LOADER, "LOADER", null, null, pinHash, null, DEPOT, null);
        insertUser(jdbc, OTHER_DEPOT_LOADER, "LOADER", null, null, encoder.encode("4321"), null, OTHER_DEPOT, null);
        insertUser(jdbc, USED_DRIVER, "DRIVER", null, "DRV-9001", pinHash, null, null, USED_VEHICLE);
        insertUser(jdbc, IDLE_DRIVER, "DRIVER", null, "DRV-9002", pinHash, null, null, IDLE_VEHICLE);
    }

    private static void insertUser(JdbcTemplate jdbc, String id, String role, String email, String staffId,
            String secretHash, String outletId, String depot, String vehicleId) {
        jdbc.update("""
                INSERT INTO users (id, name, role, email, staff_id, secret_hash, outlet_id, depot, vehicle_id, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, true)""", id, "Test " + id, role, email, staffId, secretHash,
                outletId, depot, vehicleId);
    }
}
