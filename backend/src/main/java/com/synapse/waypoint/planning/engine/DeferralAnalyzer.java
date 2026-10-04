package com.synapse.waypoint.planning.engine;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.synapse.waypoint.planning.domain.DeferralKind;
import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.input.OrderInput;
import com.synapse.waypoint.planning.engine.input.PlanningInput;
import com.synapse.waypoint.planning.engine.input.VehicleInput;
import com.synapse.waypoint.planning.engine.rule.RuleChecker;
import com.synapse.waypoint.planning.engine.rule.RuleViolation;

/**
 * Explains why an order is left out. UNAVOIDABLE: no available vehicle could carry it alone on an empty
 * trip. CHOSEN: some vehicle could, but they filled up with higher-priority orders. The deciding rule is
 * the one shared by all vehicles (unavoidable, else NO_VEHICLE_FITS), FRIDGE_CAPACITY for chilled orders,
 * or the rule that blocked the order's last placement attempt.
 */
public class DeferralAnalyzer {

    private static final int DAYS_TO_NEW_DATE = 1;

    private final PlanningInput input;
    private final PriorityScorer scorer;
    private final RuleChecker checker;

    public DeferralAnalyzer(PlanningInput input, PriorityScorer scorer, RuleChecker checker) {
        this.input = input;
        this.scorer = scorer;
        this.checker = checker;
    }

    public PlannedDeferral analyze(UnplacedOrder unplaced) {
        OrderInput order = unplaced.order();
        List<RuleViolation> aloneBlocks = blocksWhenAlone(order);
        boolean unavoidable = aloneBlocks.size() == input.vehicles().size();
        RuleCode rule = unavoidable ? unavoidableRule(aloneBlocks) : chosenRule(unplaced);
        DeferralKind kind = unavoidable ? DeferralKind.UNAVOIDABLE : DeferralKind.CHOSEN;
        String reason = unavoidable ? DeferralReasons.unavoidable(rule) : DeferralReasons.chosen(rule, fridgeVehicles());
        return new PlannedDeferral(order, kind, rule, reason, scorer.score(order), order.daysSinceLastServed(),
                input.runDate().plusDays(DAYS_TO_NEW_DATE), order.deferredYesterday());
    }

    /** One violation per vehicle that cannot carry the order alone on an empty trip. */
    private List<RuleViolation> blocksWhenAlone(OrderInput order) {
        StopCandidate stop = new StopCandidate(order, input.outletOf(order));
        return input.vehicles().stream()
                .map(vehicle -> checker.firstViolation(VehicleDay.idle(vehicle).withTrip(new TripDraft(List.of(stop)))))
                .flatMap(Optional::stream)
                .toList();
    }

    private RuleCode unavoidableRule(List<RuleViolation> aloneBlocks) {
        Set<RuleCode> rules = aloneBlocks.stream().map(RuleViolation::rule).collect(Collectors.toSet());
        return rules.size() == 1 ? rules.iterator().next() : RuleCode.NO_VEHICLE_FITS;
    }

    private RuleCode chosenRule(UnplacedOrder unplaced) {
        if (unplaced.order().isChilled()) {
            return RuleCode.FRIDGE_CAPACITY;
        }
        return unplaced.lastBlock().map(RuleViolation::rule).orElse(RuleCode.NO_VEHICLE_FITS);
    }

    private int fridgeVehicles() {
        return (int) input.vehicles().stream().filter(VehicleInput::isReefer).count();
    }
}
