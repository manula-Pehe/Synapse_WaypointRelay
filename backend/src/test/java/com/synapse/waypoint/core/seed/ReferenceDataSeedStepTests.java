package com.synapse.waypoint.core.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.reference.entity.DistrictTravelId;
import com.synapse.waypoint.core.reference.entity.Outlet;
import com.synapse.waypoint.core.reference.entity.ServiceAllowanceId;
import com.synapse.waypoint.core.reference.repository.DistrictTravelRepository;
import com.synapse.waypoint.core.reference.repository.OutletRepository;
import com.synapse.waypoint.core.reference.repository.ServiceAllowanceRepository;
import com.synapse.waypoint.core.reference.repository.VehicleRepository;

/** Loads the invented CSVs in src/test/resources/seed. */
@SpringBootTest
@TestPropertySource(properties = "app.data-dir=src/test/resources/seed")
@Transactional
class ReferenceDataSeedStepTests {

    @Autowired ReferenceDataSeedStep step;
    @Autowired OutletRepository outlets;
    @Autowired VehicleRepository vehicles;
    @Autowired DistrictTravelRepository travel;
    @Autowired ServiceAllowanceRepository allowances;

    @Test
    void shouldLoadEveryReferenceRow() {
        step.run();

        assertThat(outlets.count()).isEqualTo(3);
        assertThat(vehicles.count()).isEqualTo(3);
        assertThat(travel.count()).isEqualTo(2);
        assertThat(allowances.count()).isEqualTo(3);
    }

    @Test
    void shouldSplitMallWindowAndLeaveOthersEmpty() {
        step.run();

        Outlet mall = outlets.findById("OUT003").orElseThrow();
        Outlet plain = outlets.findById("OUT001").orElseThrow();
        assertThat(mall.getMallWindowOpen()).isEqualTo(LocalTime.of(10, 0));
        assertThat(mall.getMallWindowClose()).isEqualTo(LocalTime.of(12, 30));
        assertThat(plain.getMallWindowOpen()).isNull();
        assertThat(plain.getWindowOpen()).isEqualTo(LocalTime.of(5, 0));
    }

    @Test
    void shouldMapVehicleTravelAndAllowanceValues() {
        step.run();

        assertThat(vehicles.findById("VEH001").orElseThrow().getWeightCapKg()).isEqualByComparingTo("1500.5");
        assertThat(travel.findById(new DistrictTravelId("Mallton", "Peliyagoda")).orElseThrow().getOutboundMinutes())
                .isEqualTo(45);
        assertThat(allowances.findById(new ServiceAllowanceId("Style", "mall_bay")).orElseThrow().getMinutes())
                .isEqualTo(25);
    }
}
