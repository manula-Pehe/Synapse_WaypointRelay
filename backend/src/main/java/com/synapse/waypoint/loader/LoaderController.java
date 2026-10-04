package com.synapse.waypoint.loader;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.time.DemoClock;

@RestController
@RequestMapping("/api/loader")
public class LoaderController {
    public record FridgeCheckRequest(boolean running, BigDecimal tempC, boolean doorsOk) {}
    public record ShortfallRequest(int missingUnits, String reason, String note) {}
    public record HandoverRequest(String driverStaffId) {}

    private final LoaderService loader;
    private final DemoClock clock;

    public LoaderController(LoaderService loader, DemoClock clock) {
        this.loader = loader;
        this.clock = clock;
    }

    @GetMapping("/trips")
    public LoaderService.TripList trips(@RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate) {
        return loader.trips(runDate == null ? clock.runDate() : runDate);
    }

    @GetMapping("/trips/{id}")
    public LoaderService.TripDetail trip(@PathVariable String id) { return loader.trip(id); }

    @PostMapping("/trips/{id}/fridge-check")
    public LoaderService.FridgeCheck fridgeCheck(@PathVariable String id, @RequestBody FridgeCheckRequest request) {
        return loader.fridgeCheck(id, request.running(), request.tempC(), request.doorsOk());
    }

    @PostMapping("/stops/{stopId}/tick")
    public LoaderService.TripDetail tick(@PathVariable String stopId) { return loader.tick(stopId); }

    @PostMapping("/stops/{stopId}/shortfall")
    public LoaderService.ShortfallResult shortfall(@PathVariable String stopId, @RequestBody ShortfallRequest request) {
        return loader.shortfall(stopId, request.missingUnits(), request.reason(), request.note());
    }

    @PostMapping("/trips/{id}/handover")
    public LoaderService.HandoverResult handover(@PathVariable String id, @RequestBody HandoverRequest request) {
        return loader.handover(id, request.driverStaffId());
    }
}
