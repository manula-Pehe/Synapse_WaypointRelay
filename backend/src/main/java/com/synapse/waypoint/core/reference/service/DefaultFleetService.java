package com.synapse.waypoint.core.reference.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.time.ApiTimestamp;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.entity.OrderRun;
import com.synapse.waypoint.core.order.entity.OrderRunId;
import com.synapse.waypoint.core.order.repository.OrderRunRepository;
import com.synapse.waypoint.core.reference.dto.ConfirmFleetRequest;
import com.synapse.waypoint.core.reference.dto.FleetConfirmationDto;
import com.synapse.waypoint.core.reference.dto.FleetCountsDto;
import com.synapse.waypoint.core.reference.dto.FleetDto;
import com.synapse.waypoint.core.reference.dto.UpdateAvailabilityRequest;
import com.synapse.waypoint.core.reference.dto.VehicleDto;
import com.synapse.waypoint.core.reference.entity.AvailabilityStatus;
import com.synapse.waypoint.core.reference.entity.Vehicle;
import com.synapse.waypoint.core.reference.entity.VehicleAvailability;
import com.synapse.waypoint.core.reference.entity.VehicleAvailabilityId;
import com.synapse.waypoint.core.reference.repository.VehicleAvailabilityRepository;
import com.synapse.waypoint.core.reference.repository.VehicleRepository;

@Service
@Transactional
class DefaultFleetService implements FleetService {

    private static final String VEHICLE = "Vehicle";

    private final ReferenceService reference;
    private final VehicleRepository vehicles;
    private final VehicleAvailabilityRepository availability;
    private final VehicleMapper vehicleMapper;
    private final OrderRunRepository runs;
    private final DispatcherDepotScope depotScope;
    private final DemoClock clock;
    private final CurrentUser currentUser;

    DefaultFleetService(ReferenceService reference, VehicleRepository vehicles,
            VehicleAvailabilityRepository availability, VehicleMapper vehicleMapper, OrderRunRepository runs,
            DispatcherDepotScope depotScope, DemoClock clock, CurrentUser currentUser) {
        this.reference = reference;
        this.vehicles = vehicles;
        this.availability = availability;
        this.vehicleMapper = vehicleMapper;
        this.runs = runs;
        this.depotScope = depotScope;
        this.clock = clock;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public FleetDto view(LocalDate runDate, String depot) {
        String depotName = depotScope.resolve(depot);
        List<VehicleDto> depotVehicles = reference.vehicles(runDate, depotName);
        OrderRun run = runs.findByRunDateAndDepot(runDate, depotName).orElse(null);
        return new FleetDto(depotVehicles, run == null ? null : ApiTimestamp.of(run.getFleetConfirmedAt()),
                run == null ? null : run.getFleetConfirmedBy(), FleetCountsDto.of(depotVehicles));
    }

    @Override
    public VehicleDto setAvailability(String vehicleId, UpdateAvailabilityRequest request) {
        String reason = reasonFor(request);
        Vehicle vehicle = vehicles.findById(vehicleId).orElseThrow(() -> new NotFoundException(VEHICLE, vehicleId));
        depotScope.requireInScope(vehicle.getDepot());

        VehicleAvailabilityId id = new VehicleAvailabilityId(vehicleId, request.runDate());
        String userId = currentUser.idIfSignedIn().orElse(null);
        VehicleAvailability row = availability.findById(id).orElse(null);
        if (row == null) {
            availability.saveAndFlush(new VehicleAvailability(id, request.status(), reason, userId,
                    clock.now()));
        } else {
            row.change(request.status(), reason, userId, clock.now());
            availability.saveAndFlush(row);
        }
        return vehicleMapper.toDtos(List.of(vehicle), request.runDate()).get(0);
    }

    @Override
    public FleetConfirmationDto confirm(ConfirmFleetRequest request) {
        String depotName = depotScope.resolve(request.depot());
        runs.createIfAbsent(request.runDate(), depotName);
        OrderRun run = runs.findForUpdate(new OrderRunId(request.runDate(), depotName)).orElseThrow();
        run.confirmFleet(currentUser.idIfSignedIn().orElse(null), clock.now());
        runs.saveAndFlush(run);
        return new FleetConfirmationDto(ApiTimestamp.of(run.getFleetConfirmedAt()), run.getFleetConfirmedBy());
    }

    /** The reason to store: none for AVAILABLE, otherwise a required, non-blank text. */
    private static String reasonFor(UpdateAvailabilityRequest request) {
        if (request.status() == AvailabilityStatus.AVAILABLE) {
            return null;
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION, "Give a reason.",
                    Map.of("reason", "required unless the vehicle is available"));
        }
        return request.reason().strip();
    }
}
