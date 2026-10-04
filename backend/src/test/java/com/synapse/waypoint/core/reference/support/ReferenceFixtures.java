package com.synapse.waypoint.core.reference.support;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;

/** Invented reference data for tests (vehicles, availability, travel times). */
public final class ReferenceFixtures {

    public static final String DEPOT = "Testdepot";
    public static final String OTHER_DEPOT = "Otherdepot";
    public static final LocalDate RUN_DATE = LocalDate.parse("2026-10-01");

    private ReferenceFixtures() {
    }

    public static void insertVehicle(JdbcTemplate jdbc, String id, String temp, String depot) {
        jdbc.update("""
                INSERT INTO vehicles (id, type, temp, weight_cap_kg, volume_cap_m3, fuel_type, km_per_l,
                                      weekly_fuel_quota_l, depot)
                VALUES (?, 'van', ?, 1000, 7, 'diesel', 9.5, 300, ?)""", id, temp, depot);
    }

    public static void insertAvailability(JdbcTemplate jdbc, String vehicleId, LocalDate runDate, String status,
            String reason) {
        jdbc.update("""
                INSERT INTO vehicle_availability (vehicle_id, run_date, status, reason, updated_at)
                VALUES (?, ?, ?, ?, now())""", vehicleId, runDate, status, reason);
    }

    public static void insertTravel(JdbcTemplate jdbc, String district, String depot) {
        jdbc.update("""
                INSERT INTO district_travel (district, depot, road_class, free_flow_kmh, depot_to_district_km,
                                             outbound_min, inter_stop_km, inter_stop_min)
                VALUES (?, ?, 'highway', 60, 30, 45, 2.5, 6)""", district, depot);
    }

    public static void upsertServiceAllowance(JdbcTemplate jdbc, String brand, String dockType, int minutes) {
        jdbc.update("""
                INSERT INTO service_allowance (brand, dock_type, minutes) VALUES (?, ?, ?)
                ON CONFLICT (brand, dock_type) DO UPDATE SET minutes = EXCLUDED.minutes""", brand, dockType, minutes);
    }

    public static void insertFuelUsage(JdbcTemplate jdbc, String vehicleId, int isoYear, int isoWeek, String litres) {
        jdbc.update("INSERT INTO fuel_usage (vehicle_id, iso_year, iso_week, litres_used) VALUES (?, ?, ?, ?::numeric)",
                vehicleId, isoYear, isoWeek, litres);
    }
}
