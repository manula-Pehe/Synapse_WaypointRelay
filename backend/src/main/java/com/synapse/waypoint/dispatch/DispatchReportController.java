package com.synapse.waypoint.dispatch;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.time.DemoClock;

@RestController
@RequestMapping("/api/dispatch/reports")
class DispatchReportController {

    private final DispatchReportService reports;
    private final DemoClock clock;

    DispatchReportController(DispatchReportService reports, DemoClock clock) {
        this.reports = reports;
        this.clock = clock;
    }

    @GetMapping("/run")
    RunReportDto run(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate,
            @RequestParam(required = false) String depot) {
        return reports.read(runDate == null ? clock.runDate() : runDate, depot);
    }
}
