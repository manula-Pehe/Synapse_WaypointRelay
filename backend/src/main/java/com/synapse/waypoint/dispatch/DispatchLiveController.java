package com.synapse.waypoint.dispatch;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.time.DemoClock;

@RestController
@RequestMapping("/api/dispatch")
class DispatchLiveController {

    private final DispatchLiveService live;
    private final DemoClock clock;

    DispatchLiveController(DispatchLiveService live, DemoClock clock) {
        this.live = live;
        this.clock = clock;
    }

    @GetMapping("/live")
    LiveBoardDto live(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate,
            @RequestParam(required = false) String depot) {
        return live.read(runDate == null ? clock.runDate() : runDate, depot);
    }
}
