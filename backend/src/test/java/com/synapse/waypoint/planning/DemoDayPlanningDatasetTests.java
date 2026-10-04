package com.synapse.waypoint.planning;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.seed.DemoDay;
import com.synapse.waypoint.common.seed.SeedRunner;
import com.synapse.waypoint.core.order.dto.OrderFilters;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.planning.engine.PlannedTrip;
import com.synapse.waypoint.planning.engine.PlanningEngine;
import com.synapse.waypoint.planning.engine.PlanningResult;
import com.synapse.waypoint.planning.engine.input.OrderInput;
import com.synapse.waypoint.planning.engine.input.PlanningInput;
import com.synapse.waypoint.planning.input.PlanningInputLoader;

/**
 * Plans the seeded demo day (booklet scenario S1 at Peliyagoda) from the real competition files.
 * Runs only where {@code ./data} exists, so it is skipped in CI. It asserts counts and rule checks only and
 * writes the allocation to /tmp in the task2b submission format so the organisers' checker can verify it.
 * That file is dataset-derived: never commit it.
 */
@SpringBootTest
@TestPropertySource(properties = "app.data-dir=../data")
@Transactional
@EnabledIf("datasetIsPresent")
class DemoDayPlanningDatasetTests {

    private static final LocalDate RUN_DATE = LocalDate.of(2026, 10, 1);
    private static final String DEPOT = "Peliyagoda";
    private static final Path SUBMISSION_FILE = Path.of("/tmp/waypoint_task2b_S1.csv");
    private static final String SUBMISSION_HEADER = "scenario,order_ref,outlet_id,decision,vehicle_id,trip_id";

    @Autowired SeedRunner seedRunner;
    @Autowired OrderService orderService;
    @Autowired PlanningInputLoader inputLoader;

    static boolean datasetIsPresent() {
        return Files.isRegularFile(Path.of("../data/Test Data/task2b_peak_day_scenarios.csv"))
                && Files.isRegularFile(Path.of("../data/General Data/outlets.csv"));
    }

    @Test
    void shouldPlanTheDemoDayWithoutRuleViolations() throws IOException {
        PlanningInput input = loadDemoDayWithEveryOrderConfirmed();

        PlanningResult result = new PlanningEngine().plan(input);

        System.out.printf("S1 %s: orders=%d served=%d deferred=%d (unavoidable=%d, chosen=%d) violations=%d fridge=%d/%d%n",
                DEPOT, input.orders().size(), result.summary().served(), result.summary().deferred(),
                result.summary().unavoidable(), result.summary().chosen(), result.summary().violations(),
                result.summary().fridgeVehiclesUsed(), result.summary().fridgeVehiclesAvailable());
        System.out.printf("S1 deferrals by rule: %s; late-arrival warnings=%d%n",
                result.deferrals().stream().collect(Collectors.groupingBy(
                        deferral -> deferral.kind() + "/" + deferral.rule(), TreeMap::new, Collectors.counting())),
                result.warnings().size());
        writeSubmission(input, result);

        assertThat(result.violations()).isEmpty();
        assertThat(result.summary().served() + result.summary().deferred()).isEqualTo(input.orders().size());
        assertThat(result.summary().fridgeVehiclesUsed()).isLessThanOrEqualTo(result.summary().fridgeVehiclesAvailable());
    }

    private PlanningInput loadDemoDayWithEveryOrderConfirmed() {
        seedRunner.runOnce();
        orderService.findByRun(RUN_DATE, DEPOT, new OrderFilters(OrderStatus.PREPARED, null, null, null))
                .forEach(order -> orderService.autoConfirm(order.id()));
        return inputLoader.load(RUN_DATE, DEPOT);
    }

    private void writeSubmission(PlanningInput input, PlanningResult result) throws IOException {
        Map<String, PlannedTrip> tripByOrderId = new HashMap<>();
        result.trips().forEach(trip -> trip.stops().forEach(stop -> tripByOrderId.put(stop.order().id(), trip)));
        List<String> lines = new ArrayList<>(List.of(SUBMISSION_HEADER));
        for (OrderInput order : input.orders()) {
            PlannedTrip trip = tripByOrderId.get(order.id());
            String assignment = trip == null ? "deferred,," : "served," + trip.vehicleId() + "," + trip.tripNo();
            lines.add(String.join(",", DemoDay.SCENARIO, order.ref(), order.outletId(), assignment));
        }
        Files.write(SUBMISSION_FILE, lines);
    }
}
