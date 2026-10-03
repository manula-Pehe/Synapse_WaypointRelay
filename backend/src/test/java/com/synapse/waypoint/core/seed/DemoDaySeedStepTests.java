package com.synapse.waypoint.core.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderEventRepository;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.reference.entity.AvailabilityStatus;
import com.synapse.waypoint.core.reference.entity.VehicleAvailabilityId;
import com.synapse.waypoint.core.reference.repository.VehicleAvailabilityRepository;

/** Loads the demo day from the invented CSVs in src/test/resources/seed (only scenario S1 counts). */
@SpringBootTest
@TestPropertySource(properties = "app.data-dir=src/test/resources/seed")
@Transactional
class DemoDaySeedStepTests {

    @Autowired ReferenceDataSeedStep referenceData;
    @Autowired FleetAvailabilitySeedStep fleet;
    @Autowired DemoOrdersSeedStep demoOrders;
    @Autowired VehicleAvailabilityRepository availability;
    @Autowired OrderRepository orders;
    @Autowired OrderEventRepository events;
    @Autowired DemoClock clock;

    @BeforeEach
    void loadReferenceData() {
        referenceData.run();
    }

    @Test
    void shouldLoadFleetAvailabilityForTheRunDateOnly() {
        fleet.run();

        assertThat(availability.count()).isEqualTo(2);
        assertThat(availability.findById(new VehicleAvailabilityId("VEH001", clock.runDate())).orElseThrow().getStatus())
                .isEqualTo(AvailabilityStatus.AVAILABLE);
        assertThat(availability.findById(new VehicleAvailabilityId("VEH036", clock.runDate())).orElseThrow().getStatus())
                .isEqualTo(AvailabilityStatus.IN_WORKSHOP);
    }

    @Test
    void shouldLoadOnlyScenarioOrders() {
        demoOrders.run();

        assertThat(orders.count()).isEqualTo(4);
        assertThat(orders.findAll()).extracting(Order::getRef).doesNotContain("T-900");
    }

    @Test
    void shouldStartDemoStoreOrdersPreparedAndAllOthersConfirmed() {
        demoOrders.run();

        assertThat(orders.findAll()).allSatisfy(order -> assertThat(order.getStatus())
                .isEqualTo("OUT001".equals(order.getOutletId()) ? OrderStatus.PREPARED : OrderStatus.CONFIRMED));
        assertThat(orders.findAll().stream().filter(order -> order.getStatus() == OrderStatus.PREPARED)).hasSize(2);
    }

    @Test
    void shouldMapOrderValues() {
        demoOrders.run();

        Order chilled = byRef("T-001");
        assertThat(chilled.getTemperatureRequirement()).isEqualTo(TemperatureRequirement.CHILLED);
        assertThat(chilled.getSource()).isEqualTo(OrderSource.SEED);
        assertThat(chilled.getRunDate()).isEqualTo(clock.runDate());
        assertThat(chilled.getWeightKg()).isEqualByComparingTo("310.5");
        assertThat(chilled.getUnits()).isEqualTo(40);
        assertThat(chilled.isDeferredYesterday()).isFalse();

        Order deferred = byRef("T-002");
        assertThat(deferred.getTemperatureRequirement()).isEqualTo(TemperatureRequirement.AMBIENT);
        assertThat(deferred.isDeferredYesterday()).isTrue();
        assertThat(deferred.getDaysSinceLastServed()).isEqualTo(2);
        assertThat(byRef("T-004").getDaysSinceLastServed()).isEqualTo(7);
    }

    @Test
    void shouldWriteExactlyOneHistoryRowPerOrder() {
        demoOrders.run();

        assertThat(events.count()).isEqualTo(4);
        assertThat(events.findByOrderIdOrderByAtAscIdAsc(byRef("T-001").getId())).singleElement().satisfies(event -> {
            assertThat(event.getType()).isEqualTo("PREPARED");
            assertThat(event.getFromStatus()).isNull();
            assertThat(event.getToStatus()).isEqualTo(OrderStatus.PREPARED);
            assertThat(event.getActorUserId()).isNull();
        });
        assertThat(events.findByOrderIdOrderByAtAscIdAsc(byRef("T-003").getId())).singleElement()
                .extracting(event -> event.getToStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    private Order byRef(String ref) {
        return orders.findAll().stream().filter(order -> order.getRef().equals(ref)).findFirst().orElseThrow();
    }
}
