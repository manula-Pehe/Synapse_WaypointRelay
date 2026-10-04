package com.synapse.waypoint.driver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.dto.ListResponse;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.driver.dto.ReportProblemRequest;
import com.synapse.waypoint.driver.dto.VehicleProblemDto;
import com.synapse.waypoint.driver.service.VehicleProblemService;

/**
 * Vehicle problems from the cab — docs/api.md §7, F10 (R9, R9ok).
 *
 * The driver's report and dispatch's reply are the two ends of one thread: the driver posts, dispatch
 * answers on the same problem, and the driver reads the answer on the same screen (R9ok) so nobody
 * has to phone while driving.
 */
@RestController
@RequestMapping("/api")
class VehicleProblemController {

    private final VehicleProblemService problems;
    private final CurrentUser currentUser;
    private final DemoClock clock;

    VehicleProblemController(VehicleProblemService problems, CurrentUser currentUser, DemoClock clock) {
        this.problems = problems;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    /** R9 - a breakdown, a fridge fault or an accident, in a few taps. */
    @PostMapping("/driver/problems")
    VehicleProblemDto report(@RequestBody ReportProblemRequest request) {
        return problems.report(currentUser.id(), currentUser.vehicleId().orElse(null), request);
    }

    /** R9ok - the problems on this vehicle, so the driver can see whether dispatch has answered. */
    @GetMapping("/driver/problems")
    ListResponse<VehicleProblemDto> mine() {
        return ListResponse.of(problems.forVehicle(currentUser.vehicleId().orElse(null)));
    }

    /** Dispatch's written answer, shown on the driver's screen. */
    @PostMapping("/dispatch/problems/{id}/reply")
    VehicleProblemDto reply(@PathVariable String id, @RequestBody ReplyRequest request) {
        return problems.reply(id, request.text());
    }

    /** What dispatch sees: everything still open, newest first. */
    @GetMapping("/dispatch/problems")
    ListResponse<VehicleProblemDto> open() {
        return ListResponse.of(problems.open(clock.runDate()));
    }

    /** Dispatch's instruction; an empty answer is a validation error rather than a blank screen. */
    record ReplyRequest(String text) {
    }
}