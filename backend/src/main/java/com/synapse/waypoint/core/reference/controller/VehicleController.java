package com.synapse.waypoint.core.reference.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.dto.ListResponse;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.reference.dto.VehicleDto;
import com.synapse.waypoint.core.reference.service.ReferenceService;

/** Vehicles with their availability on a run date — docs/api.md §3. Open to every signed-in role. */
@RestController
@RequestMapping("/api/vehicles")
class VehicleController {

    private final ReferenceService reference;
    private final DemoClock clock;

    VehicleController(ReferenceService reference, DemoClock clock) {
        this.reference = reference;
        this.clock = clock;
    }

    /** {@code runDate} defaults to the current run date. */
    @GetMapping
    ListResponse<VehicleDto> list(@RequestParam(required = false) String depot,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate) {
        return ListResponse.of(reference.vehicles(runDate != null ? runDate : clock.runDate(), depot));
    }
}
