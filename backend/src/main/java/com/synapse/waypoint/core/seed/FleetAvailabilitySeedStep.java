package com.synapse.waypoint.core.seed;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.seed.DemoDay;
import com.synapse.waypoint.common.seed.SeedStep;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.reference.entity.AvailabilityStatus;
import com.synapse.waypoint.core.reference.entity.VehicleAvailability;
import com.synapse.waypoint.core.reference.entity.VehicleAvailabilityId;
import com.synapse.waypoint.core.reference.repository.VehicleAvailabilityRepository;

/** Loads which vehicles are available on the demo run date. */
@Component
class FleetAvailabilitySeedStep implements SeedStep {

    static final int ORDER = 20;
    static final String FLEET_FILE = "task2b_peak_day_fleet.csv";

    private final DatasetLocator locator;
    private final CsvReader reader;
    private final VehicleAvailabilityRepository availability;
    private final DemoClock clock;

    FleetAvailabilitySeedStep(DatasetLocator locator, CsvReader reader, VehicleAvailabilityRepository availability,
            DemoClock clock) {
        this.locator = locator;
        this.reader = reader;
        this.availability = availability;
        this.clock = clock;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public String name() {
        return "demo day fleet availability";
    }

    @Override
    public void run() {
        LocalDate runDate = clock.runDate();
        availability.saveAll(reader.read(locator.locate(FLEET_FILE)).stream()
                .filter(row -> DemoDay.SCENARIO.equals(row.text("scenario")))
                .map(row -> toAvailability(row, runDate))
                .toList());
    }

    private VehicleAvailability toAvailability(CsvRow row, LocalDate runDate) {
        return new VehicleAvailability(new VehicleAvailabilityId(row.text("vehicle_id"), runDate),
                status(row), null, null, clock.now());
    }

    private AvailabilityStatus status(CsvRow row) {
        return switch (row.text("status").toLowerCase()) {
            case "available" -> AvailabilityStatus.AVAILABLE;
            case "in_workshop" -> AvailabilityStatus.IN_WORKSHOP;
            default -> throw row.invalid("status", "must be available or in_workshop: '" + row.text("status") + "'");
        };
    }
}
