package com.synapse.waypoint.planning.engine;

import com.synapse.waypoint.planning.domain.RuleCode;

/** Plain-language explanations for deferrals, shown to dispatchers and stores. */
final class DeferralReasons {

    private DeferralReasons() {
    }

    /** No vehicle could carry the order even on its own; {@code rule} is what stopped every vehicle. */
    static String unavoidable(RuleCode rule) {
        return switch (rule) {
            case OVER_WEIGHT -> "The order is heavier than any available vehicle can carry.";
            case OVER_VOLUME -> "The order is bigger than any available vehicle can carry.";
            case FRIDGE_REQUIRED -> "The order is chilled and no fridge vehicle is available.";
            case VAN_ONLY -> "The outlet can only be reached by a van and no van is available.";
            case WRONG_DEPOT -> "No available vehicle belongs to this outlet's depot.";
            case FUEL_QUOTA -> "Every suitable vehicle has used up its weekly fuel quota.";
            case TIME_BUDGET -> "The trip is too long for any vehicle's time budget.";
            default -> "No available vehicle can carry this order.";
        };
    }

    /** A vehicle could have carried it, but the order lost out on priority; {@code rule} is the last block. */
    static String chosen(RuleCode rule, int fridgeVehicles) {
        return switch (rule) {
            case FRIDGE_CAPACITY -> fridgeVehicles == 1
                    ? "The only fridge vehicle is full before 8 AM."
                    : "All " + fridgeVehicles + " fridge vehicles are full before 8 AM.";
            case OVER_WEIGHT -> "The vehicles that could take it are already full by weight.";
            case OVER_VOLUME -> "The vehicles that could take it are already full by volume.";
            case MAX_TRIPS -> "The vehicles that could take it have no trip left today.";
            case TIME_BUDGET -> "The vehicles that could take it have no time left in their budget.";
            case FUEL_QUOTA -> "The vehicles that could take it are at their weekly fuel quota.";
            default -> "Higher-priority orders filled the vehicles that could take it.";
        };
    }
}
