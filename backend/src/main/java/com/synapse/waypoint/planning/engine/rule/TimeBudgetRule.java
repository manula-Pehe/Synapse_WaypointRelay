package com.synapse.waypoint.planning.engine.rule;

import java.util.Arrays;
import java.util.Optional;

import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.domain.TripWindowType;
import com.synapse.waypoint.planning.engine.TripCalculator;
import com.synapse.waypoint.planning.engine.VehicleDay;

/** Fresh trips total at most 270 minutes (3:30–8:00 AM); Style and Tech trips at most 480. */
public class TimeBudgetRule implements PlanningRule {

    private final TripCalculator calculator;

    public TimeBudgetRule(TripCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public RuleCode code() {
        return RuleCode.TIME_BUDGET;
    }

    @Override
    public Optional<RuleViolation> check(VehicleDay day) {
        return Arrays.stream(TripWindowType.values())
                .filter(window -> calculator.minutesInWindow(day, window) > window.budgetMinutes())
                .findFirst()
                .map(window -> new RuleViolation(code(),
                        "The trips need " + calculator.minutesInWindow(day, window) + " minutes but "
                                + window.name().toLowerCase() + " trips have " + window.budgetMinutes() + "."));
    }
}
