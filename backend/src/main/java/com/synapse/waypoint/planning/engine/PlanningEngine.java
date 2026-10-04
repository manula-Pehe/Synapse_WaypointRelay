package com.synapse.waypoint.planning.engine;

import java.util.List;

import com.synapse.waypoint.planning.domain.DeferralKind;
import com.synapse.waypoint.planning.engine.input.PlanningInput;
import com.synapse.waypoint.planning.engine.input.VehicleInput;
import com.synapse.waypoint.planning.engine.rule.RuleChecker;
import com.synapse.waypoint.planning.engine.rule.RuleViolation;

/**
 * Entry point of the planning domain: allocates the orders, explains the deferrals, then verifies the
 * whole plan with the same rule checker. Pure and deterministic; time comes only from the input's run date.
 */
public class PlanningEngine {

    private final PriorityScorer scorer;
    private final ArrivalEstimator estimator;

    public PlanningEngine() {
        this(new PriorityScorer(), new ArrivalEstimator());
    }

    PlanningEngine(PriorityScorer scorer, ArrivalEstimator estimator) {
        this.scorer = scorer;
        this.estimator = estimator;
    }

    public PlanningResult plan(PlanningInput input) {
        TripCalculator calculator = new TripCalculator(input);
        RuleChecker checker = RuleChecker.standard(calculator);

        AllocationResult allocation = new Allocator(input, scorer, checker).allocate();
        DeferralAnalyzer analyzer = new DeferralAnalyzer(input, scorer, checker);
        List<PlannedDeferral> deferrals = allocation.unplaced().stream().map(analyzer::analyze).toList();
        List<PlannedTrip> trips = new PlanAssembler(calculator, estimator).assemble(allocation.days());
        List<RuleViolation> violations = allocation.days().stream()
                .flatMap(day -> checker.violations(day).stream())
                .toList();

        return new PlanningResult(trips, deferrals, summarize(input, trips, deferrals, allocation, violations),
                violations);
    }

    private PlanSummary summarize(PlanningInput input, List<PlannedTrip> trips, List<PlannedDeferral> deferrals,
            AllocationResult allocation, List<RuleViolation> violations) {
        int served = trips.stream().mapToInt(trip -> trip.stops().size()).sum();
        long unavoidable = deferrals.stream()
                .filter(deferral -> deferral.kind() == DeferralKind.UNAVOIDABLE)
                .count();
        int fridgeUsed = (int) allocation.days().stream().filter(day -> day.vehicle().isReefer()).count();
        int fridgeAvailable = (int) input.vehicles().stream().filter(VehicleInput::isReefer).count();
        return new PlanSummary(served, deferrals.size(), (int) unavoidable, deferrals.size() - (int) unavoidable,
                violations.size(), fridgeUsed, fridgeAvailable);
    }
}
