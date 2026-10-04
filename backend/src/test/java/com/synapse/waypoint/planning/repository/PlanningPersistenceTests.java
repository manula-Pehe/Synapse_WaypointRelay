package com.synapse.waypoint.planning.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.planning.domain.DeferralKind;
import com.synapse.waypoint.planning.domain.PlanStatus;
import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.domain.StoreChoice;
import com.synapse.waypoint.planning.domain.TripWindowType;
import com.synapse.waypoint.planning.entity.Deferral;
import com.synapse.waypoint.planning.entity.DeferralChoice;
import com.synapse.waypoint.planning.entity.Plan;
import com.synapse.waypoint.planning.entity.Stop;
import com.synapse.waypoint.planning.entity.Trip;

/** Boots the app with ddl-auto=validate, so a mismatch between V10 and the entities fails here. */
@SpringBootTest
@Transactional
class PlanningPersistenceTests {

    private static final LocalDate RUN_DATE = LocalDate.of(2030, 1, 10);
    private static final Instant NOW = Instant.parse("2030-01-09T08:00:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired PlanRepository plans;
    @Autowired TripRepository trips;
    @Autowired StopRepository stops;
    @Autowired DeferralRepository deferrals;
    @Autowired DeferralChoiceRepository choices;

    @BeforeEach
    void insertReferenceRows() {
        jdbc.update("INSERT INTO outlets (id, brand, district, depot, dock_type, parking_constraint, window_open, window_close)"
                + " VALUES ('T-OUT', 'Fresh', 'Northvale', 'Testdepot', 'rear_dock', 'normal', '04:00', '08:00')");
        jdbc.update("INSERT INTO vehicles (id, type, temp, weight_cap_kg, volume_cap_m3, fuel_type, km_per_l,"
                + " weekly_fuel_quota_l, depot) VALUES ('T-VEH', 'van', 'reefer', 1000, 6, 'diesel', 8, 500, 'Testdepot')");
        jdbc.update("INSERT INTO users (id, name, role, outlet_id, secret_hash, created_at) VALUES ('T-USR', 'Test', 'STORE_MANAGER', 'T-OUT', 'x', now())");
        jdbc.update("INSERT INTO orders (id, ref, outlet_id, brand, temp_requirement, units, weight_kg, volume_m3, run_date,"
                + " status, source) VALUES ('T-ORD', 'T-1', 'T-OUT', 'Fresh', 'CHILLED', 10, 100, 1, ?, 'CONFIRMED', 'SEED')",
                RUN_DATE);
    }

    @Test
    void shouldRoundTripAPlanWithTripsStopsAndDeferrals() {
        plans.save(new Plan("T-PLAN", RUN_DATE, "Testdepot", 1, PlanStatus.DRAFT, Map.of("served", 1), null, null,
                NOW, "T-USR"));
        trips.save(new Trip("T-TRIP", "T-PLAN", "T-VEH", 1, "Fresh", "Northvale", TripWindowType.FRESH, NOW, 64,
                new BigDecimal("100.00"), new BigDecimal("1.000"), new BigDecimal("42.50")));
        stops.save(new Stop("T-STOP", "T-TRIP", "T-ORD", 1, 1, NOW, NOW.plusSeconds(1800), new BigDecimal("0.250")));
        deferrals.save(new Deferral("T-DFR", "T-PLAN", "T-ORD", DeferralKind.CHOSEN, RuleCode.FRIDGE_CAPACITY,
                "All fridge vehicles are full.", 3, 1, RUN_DATE.plusDays(1), true));
        choices.save(new DeferralChoice("T-CHC", "T-DFR", StoreChoice.REDUCE, 5, "T-USR", NOW));
        entityManager.flush();
        entityManager.clear();

        assertThat(plans.findFirstByRunDateAndDepotIgnoreCaseOrderByVersionDesc(RUN_DATE, "testdepot"))
                .get().extracting(Plan::getStatus, plan -> plan.getSummary().get("served"))
                .containsExactly(PlanStatus.DRAFT, 1);
        assertThat(trips.findByPlanIdOrderByVehicleIdAscTripNoAsc("T-PLAN")).extracting(Trip::getKm)
                .containsExactly(new BigDecimal("42.50"));
        assertThat(stops.findByTripIdInOrderByTripIdAscSeqAsc(List.of("T-TRIP"))).hasSize(1);
        assertThat(deferrals.findByOrderId("T-ORD")).extracting(Deferral::getRule, Deferral::isNeedsDecision)
                .containsExactly(tuple(RuleCode.FRIDGE_CAPACITY, true));
        assertThat(choices.findByDeferralId("T-DFR")).extracting(DeferralChoice::getUnits).containsExactly(5);
    }

    @Test
    void shouldRefuseASecondChoiceForTheSameDeferral() {
        plans.save(new Plan("T-PLAN", RUN_DATE, "Testdepot", 1, PlanStatus.DRAFT, Map.of("served", 0), null, null,
                NOW, "T-USR"));
        deferrals.save(new Deferral("T-DFR", "T-PLAN", "T-ORD", DeferralKind.CHOSEN, RuleCode.FRIDGE_CAPACITY,
                "All fridge vehicles are full.", 3, 1, RUN_DATE.plusDays(1), false));
        choices.saveAndFlush(new DeferralChoice("T-CHC", "T-DFR", StoreChoice.KEEP, null, "T-USR", NOW));

        assertThatThrownBy(() -> choices.saveAndFlush(
                new DeferralChoice("T-CHC2", "T-DFR", StoreChoice.CANCEL, null, "T-USR", NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
