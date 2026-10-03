package com.synapse.waypoint.core.reference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.reference.dto.OutletDto;
import com.synapse.waypoint.core.reference.dto.TravelDto;
import com.synapse.waypoint.core.reference.dto.VehicleDto;
import com.synapse.waypoint.core.reference.entity.AvailabilityStatus;
import com.synapse.waypoint.core.reference.support.ReferenceFixtures;

/** ReferenceService lookups for other modules. Data is invented. */
@SpringBootTest
@Transactional
class ReferenceServiceTests {

    private static final String DEPOT = ReferenceFixtures.DEPOT;
    private static final String DISTRICT = "Testdistrict";

    @Autowired ReferenceService reference;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void createReferenceData() {
        OrderFixtures.insertOutlet(jdbc, "OUT991", "Fresh", DISTRICT);
        OrderFixtures.insertOutlet(jdbc, "OUT992", "Style", "Seconddistrict");
        OrderFixtures.insertOutlet(jdbc, "OUT993", "Fresh", "Otherdistrict", ReferenceFixtures.OTHER_DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, "VEH991", "reefer", DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, "VEH992", "ambient", DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, "VEH993", "reefer", ReferenceFixtures.OTHER_DEPOT);
        ReferenceFixtures.insertTravel(jdbc, DISTRICT, DEPOT);
        ReferenceFixtures.upsertServiceAllowance(jdbc, "Fresh", "rear_dock", 17);
    }

    @Test
    void shouldReturnAnOutletWithItsDisplayName() {
        OutletDto outlet = reference.outlet("OUT991");

        assertThat(outlet.name()).isEqualTo("OUT991 · Testdistrict");
        assertThat(outlet.depot()).isEqualTo(DEPOT);
        assertThat(outlet.mallWindowOpen()).isNull();
    }

    @Test
    void shouldThrowNotFoundForAnUnknownOutlet() {
        assertThatThrownBy(() -> reference.outlet("OUT000")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldListOutletsOfADepotIgnoringCase() {
        assertThat(reference.outlets("testdepot")).extracting(OutletDto::id).containsExactly("OUT991", "OUT992");
    }

    @Test
    void shouldFilterOutletsByBrand() {
        assertThat(reference.outlets(DEPOT, "style")).extracting(OutletDto::id).containsExactly("OUT992");
    }

    @Test
    void shouldListEveryDepotWhenNoneIsGiven() {
        assertThat(reference.outlets(null)).extracting(OutletDto::id).contains("OUT991", "OUT993");
    }

    @Test
    void shouldReturnAVehicleAvailableByDefaultOnTheCurrentRunDate() {
        VehicleDto vehicle = reference.vehicle("VEH991");

        assertThat(vehicle.availability()).isEqualTo(AvailabilityStatus.AVAILABLE);
        assertThat(vehicle.availabilityReason()).isNull();
    }

    @Test
    void shouldThrowNotFoundForAnUnknownVehicle() {
        assertThatThrownBy(() -> reference.vehicle("VEH000")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldLeaveOutVehiclesThatAreNotAvailableOnTheRunDate() {
        ReferenceFixtures.insertAvailability(jdbc, "VEH992", ReferenceFixtures.RUN_DATE, "IN_WORKSHOP", "Service");

        assertThat(reference.availableVehicles(ReferenceFixtures.RUN_DATE, DEPOT))
                .extracting(VehicleDto::id).containsExactly("VEH991");
    }

    @Test
    void shouldReturnTravelTimesForADistrictAndDepotIgnoringCase() {
        TravelDto travel = reference.travel("testdistrict", "testdepot");

        assertThat(travel.outboundMinutes()).isEqualTo(45);
        assertThat(travel.interStopMinutes()).isEqualTo(6);
        assertThat(travel.depotToDistrictKm()).isEqualByComparingTo("30");
    }

    @Test
    void shouldThrowNotFoundWhenThereIsNoTravelRow() {
        assertThatThrownBy(() -> reference.travel(DISTRICT, ReferenceFixtures.OTHER_DEPOT))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturnServiceMinutesForABrandAndDockType() {
        assertThat(reference.serviceMinutes("Fresh", "rear_dock")).isEqualTo(17);
    }

    @Test
    void shouldThrowNotFoundForAnUnknownServiceAllowance() {
        assertThatThrownBy(() -> reference.serviceMinutes("Fresh", "no_such_dock"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturnFuelUsedForTheWeek() {
        ReferenceFixtures.insertFuelUsage(jdbc, "VEH991", 2026, 40, "123.45");

        assertThat(reference.fuelUsed("VEH991", 2026, 40)).isEqualByComparingTo("123.45");
    }

    @Test
    void shouldReturnZeroFuelUsedWhenNothingIsRecorded() {
        assertThat(reference.fuelUsed("VEH991", 2026, 41)).isEqualByComparingTo("0");
    }
}
