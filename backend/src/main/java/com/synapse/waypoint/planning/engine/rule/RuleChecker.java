package com.synapse.waypoint.planning.engine.rule;

import java.util.List;
import java.util.Optional;

import com.synapse.waypoint.planning.engine.TripCalculator;
import com.synapse.waypoint.planning.engine.VehicleDay;

/**
 * Runs every planning rule over a vehicle's day. The allocator uses it to test candidates, the plan
 * is verified with it at the end, and a dispatcher's manual move (D5) is validated with it.
 */
public class RuleChecker {

    private final List<PlanningRule> rules;

    public RuleChecker(List<PlanningRule> rules) {
        this.rules = List.copyOf(rules);
    }

    /** All rules, cheapest checks first. */
    public static RuleChecker standard(TripCalculator calculator) {
        return new RuleChecker(List.of(
                new WrongDepotRule(),
                new BrandDistrictRule(),
                new FridgeRequiredRule(),
                new VanOnlyRule(),
                new OverWeightRule(),
                new OverVolumeRule(),
                new MaxTripsRule(),
                new TimeBudgetRule(calculator),
                new FuelQuotaRule(calculator),
                new WindowRule(calculator)));
    }

    public Optional<RuleViolation> firstViolation(VehicleDay day) {
        return rules.stream()
                .map(rule -> rule.check(day))
                .flatMap(Optional::stream)
                .findFirst();
    }

    public List<RuleViolation> violations(VehicleDay day) {
        return rules.stream()
                .map(rule -> rule.check(day))
                .flatMap(Optional::stream)
                .toList();
    }
}
