package com.synapse.waypoint.core.reference.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.dto.ListResponse;
import com.synapse.waypoint.core.reference.dto.OutletDto;
import com.synapse.waypoint.core.reference.service.ReferenceService;

/** Outlets — docs/api.md §3 (D13). Open to every signed-in role. */
@RestController
@RequestMapping("/api/outlets")
class OutletController {

    private final ReferenceService reference;

    OutletController(ReferenceService reference) {
        this.reference = reference;
    }

    @GetMapping
    ListResponse<OutletDto> list(@RequestParam(required = false) String depot,
            @RequestParam(required = false) String brand) {
        return ListResponse.of(reference.outlets(depot, brand));
    }

    @GetMapping("/{id}")
    OutletDto get(@PathVariable String id) {
        return reference.outlet(id);
    }
}
