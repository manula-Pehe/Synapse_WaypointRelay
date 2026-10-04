package com.synapse.waypoint.planning.engine.rule;

import java.math.BigDecimal;
import java.util.Optional;

import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.TripCalculator;
import com.synapse.waypoint.planning.engine.VehicleDay;

/** Fuel already used this week plus the litres of the day's trips must stay within the weekly quota. */
public class FuelQuotaRule implements PlanningRule {

    private final TripCalculator calculator;

    public FuelQuotaRule(TripCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public RuleCode code() {
        return RuleCode.FUEL_QUOTA;
    }

    @Override
    public Optional<RuleViolation> check(VehicleDay day) {
        BigDecimal weeklyTotal = day.vehicle().fuelUsedThisWeekLitres().add(calculator.litres(day));
        if (weeklyTotal.compareTo(day.vehicle().weeklyQuotaLitres()) <= 0) {
            return Optional.empty();
        }
        return Optional.of(new RuleViolation(code(),
                "Vehicle " + day.vehicle().id() + " would use more than its weekly fuel quota."));
    }
}
