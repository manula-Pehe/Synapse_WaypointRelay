package com.synapse.waypoint.core.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.reference.entity.AvailabilityStatus;
import com.synapse.waypoint.core.reference.entity.DistrictTravel;
import com.synapse.waypoint.core.reference.entity.DistrictTravelId;
import com.synapse.waypoint.core.reference.entity.Outlet;
import com.synapse.waypoint.core.reference.entity.ServiceAllowance;
import com.synapse.waypoint.core.reference.entity.ServiceAllowanceId;
import com.synapse.waypoint.core.reference.entity.Vehicle;
import com.synapse.waypoint.core.reference.entity.VehicleAvailability;
import com.synapse.waypoint.core.reference.entity.VehicleAvailabilityId;
import com.synapse.waypoint.core.reference.repository.DistrictTravelRepository;
import com.synapse.waypoint.core.reference.repository.OutletRepository;
import com.synapse.waypoint.core.reference.repository.ServiceAllowanceRepository;
import com.synapse.waypoint.core.reference.repository.VehicleAvailabilityRepository;
import com.synapse.waypoint.core.reference.repository.VehicleRepository;

/** Entities must match V1 exactly (ddl-auto=validate); this saves and reloads each one. Values are invented. */
@SpringBootTest
@Transactional
class ReferenceEntitiesPersistenceTests {

    @Autowired OutletRepository outlets;
    @Autowired VehicleRepository vehicles;
    @Autowired DistrictTravelRepository travel;
    @Autowired ServiceAllowanceRepository allowances;
    @Autowired VehicleAvailabilityRepository availability;

    @Test
    void shouldSaveAndReloadOutletWithMallWindow() {
        outlets.saveAndFlush(new Outlet("OUT990", "Style", "Testdistrict", "Kandy", "mall_bay", "mall_dock",
                LocalTime.of(10, 0), LocalTime.of(12, 0), LocalTime.of(9, 0), LocalTime.of(18, 0)));

        Outlet loaded = outlets.findById("OUT990").orElseThrow();

        assertThat(loaded.getMallWindowOpen()).isEqualTo(LocalTime.of(10, 0));
        assertThat(loaded.getWindowClose()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void shouldSaveAndReloadVehicle() {
        vehicles.saveAndFlush(new Vehicle("VEH990", "van", "reefer", new BigDecimal("1500.50"), new BigDecimal("7.250"),
                "diesel", new BigDecimal("9.50"), new BigDecimal("400.00"), "Peliyagoda"));

        Vehicle loaded = vehicles.findById("VEH990").orElseThrow();

        assertThat(loaded.getWeightCapKg()).isEqualByComparingTo("1500.5");
        assertThat(loaded.getKmPerLitre()).isEqualByComparingTo("9.5");
    }

    @Test
    void shouldSaveAndReloadDistrictTravelByCompositeKey() {
        DistrictTravelId id = new DistrictTravelId("Testdistrict", "Kandy");
        travel.saveAndFlush(new DistrictTravel(id, "highway", new BigDecimal("40.0"), new BigDecimal("12.5"), 30,
                new BigDecimal("2.0"), 5));

        assertThat(travel.findById(id)).get().satisfies(row -> {
            assertThat(row.getOutboundMinutes()).isEqualTo(30);
            assertThat(row.getInterStopMinutes()).isEqualTo(5);
        });
    }

    @Test
    void shouldSaveAndReloadServiceAllowanceByCompositeKey() {
        ServiceAllowanceId id = new ServiceAllowanceId("Fresh", "street");
        allowances.saveAndFlush(new ServiceAllowance(id, 15));

        assertThat(allowances.findById(id)).get().extracting(ServiceAllowance::getMinutes).isEqualTo(15);
    }

    @Test
    void shouldSaveAndReloadVehicleAvailability() {
        vehicles.saveAndFlush(new Vehicle("VEH991", "truck", "ambient", BigDecimal.TEN, BigDecimal.ONE, "diesel",
                BigDecimal.ONE, BigDecimal.TEN, "Kandy"));
        VehicleAvailabilityId id = new VehicleAvailabilityId("VEH991", LocalDate.parse("2026-10-01"));
        availability.saveAndFlush(new VehicleAvailability(id, AvailabilityStatus.IN_WORKSHOP, null, null, Instant.now()));

        assertThat(availability.findById(id)).get().extracting(VehicleAvailability::getStatus)
                .isEqualTo(AvailabilityStatus.IN_WORKSHOP);
    }
}
