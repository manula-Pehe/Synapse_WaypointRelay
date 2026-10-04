package com.synapse.waypoint.planning.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.planning.domain.StoreChoice;
import com.synapse.waypoint.planning.dto.StoreDeferralDto;
import com.synapse.waypoint.planning.service.DeferralChoiceService;

/** Store answers to deferrals — docs/api.md §6 (S4k, S4r, S4x, S4u, S2c). Store-manager-only by path rule. */
@RestController
@RequestMapping("/api/store/deferrals")
class StoreDeferralController {

    private final DeferralChoiceService choices;

    StoreDeferralController(DeferralChoiceService choices) {
        this.choices = choices;
    }

    record ChoiceRequest(@NotNull StoreChoice choice, Integer units) {
    }

    @PostMapping("/{id}/choice")
    StoreDeferralDto choose(@PathVariable String id, @Valid @RequestBody ChoiceRequest request) {
        return StoreDeferralDto.from(choices.choose(id, request.choice(), request.units()));
    }
}
