package com.synapse.waypoint.planning.dto;

import java.util.HashMap;
import java.util.Map;

import com.synapse.waypoint.planning.engine.PlanSummary;

/**
 * The headline numbers of a plan. Stored as JSON in {@code plans.summary}; {@code warnings} counts
 * late-arrival risks that do not break a rule.
 */
public record PlanSummaryDto(int served, int deferred, int unavoidable, int chosen, int violations,
        int fridgeVehiclesUsed, int fridgeVehiclesAvailable, int warnings) {

    private static final String SERVED = "served";
    private static final String DEFERRED = "deferred";
    private static final String UNAVOIDABLE = "unavoidable";
    private static final String CHOSEN = "chosen";
    private static final String VIOLATIONS = "violations";
    private static final String FRIDGE_USED = "fridgeVehiclesUsed";
    private static final String FRIDGE_AVAILABLE = "fridgeVehiclesAvailable";
    private static final String WARNINGS = "warnings";

    public static PlanSummaryDto of(PlanSummary summary, int warnings) {
        return new PlanSummaryDto(summary.served(), summary.deferred(), summary.unavoidable(), summary.chosen(),
                summary.violations(), summary.fridgeVehiclesUsed(), summary.fridgeVehiclesAvailable(), warnings);
    }

    public static PlanSummaryDto fromStored(Map<String, Object> stored) {
        return new PlanSummaryDto(number(stored, SERVED), number(stored, DEFERRED), number(stored, UNAVOIDABLE),
                number(stored, CHOSEN), number(stored, VIOLATIONS), number(stored, FRIDGE_USED),
                number(stored, FRIDGE_AVAILABLE), number(stored, WARNINGS));
    }

    public Map<String, Object> toStored() {
        Map<String, Object> stored = new HashMap<>();
        stored.put(SERVED, served);
        stored.put(DEFERRED, deferred);
        stored.put(UNAVOIDABLE, unavoidable);
        stored.put(CHOSEN, chosen);
        stored.put(VIOLATIONS, violations);
        stored.put(FRIDGE_USED, fridgeVehiclesUsed);
        stored.put(FRIDGE_AVAILABLE, fridgeVehiclesAvailable);
        stored.put(WARNINGS, warnings);
        return stored;
    }

    private static int number(Map<String, Object> stored, String key) {
        return stored.get(key) instanceof Number value ? value.intValue() : 0;
    }
}
