package com.synapse.waypoint.planning.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.synapse.waypoint.planning.engine.input.OrderInput;
import com.synapse.waypoint.planning.engine.input.PlanningInput;
import com.synapse.waypoint.planning.engine.input.VehicleInput;
import com.synapse.waypoint.planning.engine.rule.RuleChecker;
import com.synapse.waypoint.planning.engine.rule.RuleViolation;

/**
 * Greedy and deterministic. Orders go in by priority (then chilled first, then larger first). Each
 * order joins an existing compatible trip if one has room, otherwise opens a new trip on the smallest
 * vehicle that fits, keeping fridge vehicles free for chilled orders where possible. Every candidate
 * is accepted only if the {@link RuleChecker} finds no violation for the vehicle's whole day.
 */
public class Allocator {

    private static final Comparator<StopCandidate> BY_WINDOW_CLOSE = Comparator
            .comparing((StopCandidate stop) -> stop.window().close())
            .thenComparing(stop -> stop.window().open())
            .thenComparing(stop -> stop.order().ref());

    private final PlanningInput input;
    private final PriorityScorer scorer;
    private final RuleChecker checker;

    public Allocator(PlanningInput input, PriorityScorer scorer, RuleChecker checker) {
        this.input = input;
        this.scorer = scorer;
        this.checker = checker;
    }

    public AllocationResult allocate() {
        Map<String, VehicleDay> days = new LinkedHashMap<>();
        List<UnplacedOrder> unplaced = new ArrayList<>();
        for (OrderInput order : ordersByPlacementOrder()) {
            Attempt attempt = new Attempt(new StopCandidate(order, input.outletOf(order)));
            if (!attempt.placeInto(days)) {
                unplaced.add(new UnplacedOrder(order, attempt.lastBlock()));
            }
        }
        List<VehicleDay> used = days.values().stream().filter(day -> !day.trips().isEmpty()).toList();
        return new AllocationResult(used, unplaced);
    }

    private List<OrderInput> ordersByPlacementOrder() {
        Comparator<OrderInput> placementOrder = Comparator
                .comparingInt(scorer::score).reversed()
                .thenComparing(order -> !order.isChilled())
                .thenComparing(Comparator.comparing(OrderInput::weightKg).reversed())
                .thenComparing(Comparator.comparing(OrderInput::volumeM3).reversed())
                .thenComparing(OrderInput::ref);
        return input.orders().stream().sorted(placementOrder).toList();
    }

    /** The search for a home for one order; remembers the last rule that said no. */
    private final class Attempt {

        private final StopCandidate stop;
        private final List<VehicleInput> vehiclesByPreference;
        private RuleViolation lastBlock;

        Attempt(StopCandidate stop) {
            this.stop = stop;
            this.vehiclesByPreference = input.vehicles().stream().sorted(preferenceFor(stop.order())).toList();
        }

        Optional<RuleViolation> lastBlock() {
            return Optional.ofNullable(lastBlock);
        }

        boolean placeInto(Map<String, VehicleDay> days) {
            return joinExistingTrip(days) || openNewTrip(days);
        }

        private boolean joinExistingTrip(Map<String, VehicleDay> days) {
            for (VehicleInput vehicle : vehiclesByPreference) {
                VehicleDay day = days.get(vehicle.id());
                if (day != null && tryJoin(days, day)) {
                    return true;
                }
            }
            return false;
        }

        private boolean tryJoin(Map<String, VehicleDay> days, VehicleDay day) {
            for (int tripIndex = 0; tripIndex < day.trips().size(); tripIndex++) {
                TripDraft trip = day.trips().get(tripIndex);
                if (trip.brand() != stop.order().brand() || !trip.district().equals(stop.outlet().district())) {
                    continue;
                }
                List<StopCandidate> stops = new ArrayList<>(trip.stops());
                stops.add(stop);
                stops.sort(BY_WINDOW_CLOSE);
                VehicleDay candidate = day.withTripReplaced(tripIndex, trip.withStops(stops));
                if (accepts(candidate)) {
                    days.put(day.vehicle().id(), candidate);
                    return true;
                }
            }
            return false;
        }

        private boolean openNewTrip(Map<String, VehicleDay> days) {
            for (VehicleInput vehicle : vehiclesByPreference) {
                VehicleDay day = days.getOrDefault(vehicle.id(), VehicleDay.idle(vehicle));
                VehicleDay candidate = day.withTrip(new TripDraft(List.of(stop)));
                if (accepts(candidate)) {
                    days.put(vehicle.id(), candidate);
                    return true;
                }
            }
            return false;
        }

        private boolean accepts(VehicleDay candidate) {
            Optional<RuleViolation> violation = checker.firstViolation(candidate);
            violation.ifPresent(found -> lastBlock = found);
            return violation.isEmpty();
        }
    }

    /** Smallest vehicle first; fridge vehicles last unless the order is chilled. */
    private static Comparator<VehicleInput> preferenceFor(OrderInput order) {
        return Comparator
                .comparing((VehicleInput vehicle) -> vehicle.isReefer() && !order.isChilled())
                .thenComparing(VehicleInput::weightCapKg)
                .thenComparing(VehicleInput::volumeCapM3)
                .thenComparing(VehicleInput::id);
    }
}
