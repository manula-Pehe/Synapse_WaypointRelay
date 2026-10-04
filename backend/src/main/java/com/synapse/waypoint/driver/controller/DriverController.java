package com.synapse.waypoint.driver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.driver.dto.TodayDto;
import com.synapse.waypoint.driver.service.TodayService;

/**
 * The driver's own run — docs/api.md §7.
 *
 * Reads are always for the signed-in driver's vehicle; there is no vehicle parameter, so one driver
 * cannot ask for another's run.
 */
@RestController
@RequestMapping("/api/driver")
class DriverController {

    private final TodayService today;

    DriverController(TodayService today) {
        this.today = today;
    }

    /**
     * The whole run in one call, so the phone can cache it at sign-in and every screen after that
     * works with no signal (US-1.1).
     */
    @GetMapping("/today")
    TodayDto today() {
        return today.today();
    }

    /** R0 - the driver accepts the load, and the trip's orders go on the way. */
    @PostMapping("/trips/{id}/accept")
    TodayDto acceptTrip(@PathVariable String id) {
        return today.acceptTrip(id);
    }
}