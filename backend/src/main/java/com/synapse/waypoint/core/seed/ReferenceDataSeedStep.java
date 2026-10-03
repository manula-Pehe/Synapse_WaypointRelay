package com.synapse.waypoint.core.seed;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.seed.SeedStep;
import com.synapse.waypoint.core.reference.entity.DistrictTravel;
import com.synapse.waypoint.core.reference.entity.DistrictTravelId;
import com.synapse.waypoint.core.reference.entity.Outlet;
import com.synapse.waypoint.core.reference.entity.ServiceAllowance;
import com.synapse.waypoint.core.reference.entity.ServiceAllowanceId;
import com.synapse.waypoint.core.reference.entity.Vehicle;
import com.synapse.waypoint.core.reference.repository.DistrictTravelRepository;
import com.synapse.waypoint.core.reference.repository.OutletRepository;
import com.synapse.waypoint.core.reference.repository.ServiceAllowanceRepository;
import com.synapse.waypoint.core.reference.repository.VehicleRepository;

/** Loads outlets, vehicles, district travel and service allowances from the dataset. */
@Component
class ReferenceDataSeedStep implements SeedStep {

    static final int ORDER = 10;
    static final String OUTLETS_FILE = "outlets.csv";
    static final String VEHICLES_FILE = "vehicles.csv";
    static final String DISTRICT_TRAVEL_FILE = "district_travel.csv";
    static final String SERVICE_ALLOWANCE_FILE = "service_allowance.csv";

    private final DatasetLocator locator;
    private final CsvReader reader;
    private final OutletRepository outlets;
    private final VehicleRepository vehicles;
    private final DistrictTravelRepository travel;
    private final ServiceAllowanceRepository allowances;

    ReferenceDataSeedStep(DatasetLocator locator, CsvReader reader, OutletRepository outlets,
            VehicleRepository vehicles, DistrictTravelRepository travel, ServiceAllowanceRepository allowances) {
        this.locator = locator;
        this.reader = reader;
        this.outlets = outlets;
        this.vehicles = vehicles;
        this.travel = travel;
        this.allowances = allowances;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public String name() {
        return "reference data (outlets, vehicles, travel, service allowance)";
    }

    @Override
    public void run() {
        outlets.saveAll(rows(OUTLETS_FILE).stream().map(this::outlet).toList());
        vehicles.saveAll(rows(VEHICLES_FILE).stream().map(this::vehicle).toList());
        travel.saveAll(rows(DISTRICT_TRAVEL_FILE).stream().map(this::districtTravel).toList());
        allowances.saveAll(rows(SERVICE_ALLOWANCE_FILE).stream().map(this::serviceAllowance).toList());
    }

    private List<CsvRow> rows(String fileName) {
        return reader.read(locator.locate(fileName));
    }

    private Outlet outlet(CsvRow row) {
        Optional<MallWindow> mall = MallWindow.parse(row.optionalText("mall_window"), row, "mall_window");
        return new Outlet(row.text("outlet_id"), row.text("brand"), row.text("district"), row.text("depot"),
                row.text("dock_type"), row.text("parking_constraint"),
                mall.map(MallWindow::open).orElse(null), mall.map(MallWindow::close).orElse(null),
                row.time("window_open_time"), row.time("window_close_time"));
    }

    private Vehicle vehicle(CsvRow row) {
        return new Vehicle(row.text("vehicle_id"), row.text("type"), row.text("temp"),
                row.decimal("weight_cap_kg"), row.decimal("volume_cap_m3"), row.text("fuel_type"),
                row.decimal("km_per_l"), row.decimal("weekly_fuel_quota_l"), row.text("depot"));
    }

    private DistrictTravel districtTravel(CsvRow row) {
        return new DistrictTravel(new DistrictTravelId(row.text("district"), row.text("depot")),
                row.text("road_class"), row.decimal("free_flow_kmh"), row.decimal("depot_to_district_km"),
                row.integer("depot_to_district_freeflow_min"), row.decimal("inter_stop_km"),
                row.integer("inter_stop_freeflow_min"));
    }

    private ServiceAllowance serviceAllowance(CsvRow row) {
        return new ServiceAllowance(new ServiceAllowanceId(row.text("brand"), row.text("dock_type")),
                row.integer("service_allowance_min"));
    }
}
