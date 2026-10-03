package com.synapse.waypoint.core.reference.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.reference.dto.OutletDto;
import com.synapse.waypoint.core.reference.dto.TravelDto;
import com.synapse.waypoint.core.reference.dto.VehicleDto;
import com.synapse.waypoint.core.reference.entity.DistrictTravel;
import com.synapse.waypoint.core.reference.entity.FuelUsage;
import com.synapse.waypoint.core.reference.entity.FuelUsageId;
import com.synapse.waypoint.core.reference.entity.Outlet;
import com.synapse.waypoint.core.reference.entity.ServiceAllowance;
import com.synapse.waypoint.core.reference.entity.ServiceAllowanceId;
import com.synapse.waypoint.core.reference.entity.Vehicle;
import com.synapse.waypoint.core.reference.repository.DistrictTravelRepository;
import com.synapse.waypoint.core.reference.repository.FuelUsageRepository;
import com.synapse.waypoint.core.reference.repository.OutletRepository;
import com.synapse.waypoint.core.reference.repository.ServiceAllowanceRepository;
import com.synapse.waypoint.core.reference.repository.VehicleRepository;
import com.synapse.waypoint.core.reference.service.FleetAvailabilityPolicy.Availability;

@Service
@Transactional(readOnly = true)
class DefaultReferenceService implements ReferenceService {

    private static final String OUTLET = "Outlet";
    private static final String VEHICLE = "Vehicle";
    private static final String TRAVEL = "Travel time";
    private static final String SERVICE_ALLOWANCE = "Service time";

    private final OutletRepository outlets;
    private final VehicleRepository vehicles;
    private final DistrictTravelRepository travel;
    private final ServiceAllowanceRepository serviceAllowances;
    private final FuelUsageRepository fuelUsage;
    private final VehicleMapper vehicleMapper;
    private final FleetAvailabilityPolicy availabilityPolicy;
    private final DemoClock clock;

    DefaultReferenceService(OutletRepository outlets, VehicleRepository vehicles, DistrictTravelRepository travel,
            ServiceAllowanceRepository serviceAllowances, FuelUsageRepository fuelUsage, VehicleMapper vehicleMapper,
            FleetAvailabilityPolicy availabilityPolicy, DemoClock clock) {
        this.outlets = outlets;
        this.vehicles = vehicles;
        this.travel = travel;
        this.serviceAllowances = serviceAllowances;
        this.fuelUsage = fuelUsage;
        this.vehicleMapper = vehicleMapper;
        this.availabilityPolicy = availabilityPolicy;
        this.clock = clock;
    }

    @Override
    public OutletDto outlet(String id) {
        return outlets.findById(id).map(DefaultReferenceService::toDto)
                .orElseThrow(() -> new NotFoundException(OUTLET, id));
    }

    @Override
    public List<OutletDto> outlets(String depot) {
        return outlets(depot, null);
    }

    @Override
    public List<OutletDto> outlets(String depot, String brand) {
        List<Outlet> found = isBlank(depot) ? outlets.findAllByOrderByIdAsc()
                : outlets.findByDepotIgnoreCaseOrderByIdAsc(depot.strip());
        return found.stream()
                .filter(outlet -> isBlank(brand) || outlet.getBrand().equalsIgnoreCase(brand.strip()))
                .map(DefaultReferenceService::toDto)
                .toList();
    }

    @Override
    public VehicleDto vehicle(String id) {
        Vehicle vehicle = vehicles.findById(id).orElseThrow(() -> new NotFoundException(VEHICLE, id));
        return vehicleMapper.toDtos(List.of(vehicle), clock.runDate()).get(0);
    }

    @Override
    public List<VehicleDto> vehicles(LocalDate runDate, String depot) {
        return vehicleMapper.toDtos(vehiclesOf(depot), runDate);
    }

    @Override
    public List<VehicleDto> availableVehicles(LocalDate runDate, String depot) {
        List<Vehicle> depotVehicles = vehiclesOf(depot);
        var availability = availabilityPolicy.forVehicles(runDate, depotVehicles);
        List<Vehicle> available = depotVehicles.stream()
                .filter(vehicle -> availability.get(vehicle.getId()).isAvailable())
                .toList();
        return vehicleMapper.toDtos(available, runDate);
    }

    @Override
    public TravelDto travel(String district, String depot) {
        return travel.findByDistrictAndDepot(district, depot).map(DefaultReferenceService::toDto)
                .orElseThrow(() -> new NotFoundException(TRAVEL, district + "/" + depot));
    }

    @Override
    public int serviceMinutes(String brand, String dockType) {
        return serviceAllowances.findById(new ServiceAllowanceId(brand, dockType))
                .map(ServiceAllowance::getMinutes)
                .orElseThrow(() -> new NotFoundException(SERVICE_ALLOWANCE, brand + "/" + dockType));
    }

    @Override
    public BigDecimal fuelUsed(String vehicleId, int isoYear, int isoWeek) {
        return fuelUsage.findById(new FuelUsageId(vehicleId, isoYear, isoWeek))
                .map(FuelUsage::getLitresUsed)
                .orElse(BigDecimal.ZERO);
    }

    private List<Vehicle> vehiclesOf(String depot) {
        return isBlank(depot) ? vehicles.findAllByOrderByIdAsc()
                : vehicles.findByDepotIgnoreCaseOrderByIdAsc(depot.strip());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static OutletDto toDto(Outlet outlet) {
        return new OutletDto(outlet.getId(), OutletNames.of(outlet.getId(), outlet.getDistrict()), outlet.getBrand(),
                outlet.getDistrict(), outlet.getDepot(), outlet.getDockType(), outlet.getParkingConstraint(),
                outlet.getWindowOpen(), outlet.getWindowClose(), outlet.getMallWindowOpen(),
                outlet.getMallWindowClose());
    }

    private static TravelDto toDto(DistrictTravel row) {
        return new TravelDto(row.getId().district(), row.getId().depot(), row.getRoadClass(), row.getFreeFlowKmh(),
                row.getDepotToDistrictKm(), row.getOutboundMinutes(), row.getInterStopKm(), row.getInterStopMinutes());
    }
}
