package com.synapse.waypoint.core.reference.controller;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.reference.dto.ConfirmFleetRequest;
import com.synapse.waypoint.core.reference.dto.FleetConfirmationDto;
import com.synapse.waypoint.core.reference.dto.FleetDto;
import com.synapse.waypoint.core.reference.dto.UpdateAvailabilityRequest;
import com.synapse.waypoint.core.reference.dto.VehicleDto;
import com.synapse.waypoint.core.reference.service.FleetService;

/** Dispatcher fleet endpoints - docs/api.md §3 (D2, D2v). Dispatcher-only by path rule. */
@RestController
@RequestMapping("/api/dispatch/fleet")
class DispatchFleetController {

    private final FleetService fleet;
    private final DemoClock clock;

    DispatchFleetController(FleetService fleet, DemoClock clock) {
        this.fleet = fleet;
        this.clock = clock;
    }

    /** {@code runDate} defaults to the current run date; {@code depot} to the dispatcher's own depot. */
    @GetMapping
    FleetDto view(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate,
            @RequestParam(required = false) String depot) {
        return fleet.view(runDate != null ? runDate : clock.runDate(), depot);
    }

    /** The D2v "take off the road" / "back on the road" action. */
    @PutMapping("/{vehicleId}")
    VehicleDto setAvailability(@PathVariable String vehicleId, @Valid @RequestBody UpdateAvailabilityRequest request) {
        return fleet.setAvailability(vehicleId, request);
    }

    @PostMapping("/confirm")
    FleetConfirmationDto confirm(@Valid @RequestBody ConfirmFleetRequest request) {
        return fleet.confirm(request);
    }
}
