package com.synapse.waypoint.planning.engine;

import java.math.BigDecimal;
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
        List<OrderInput> queue = ordersByPlacementOrder();
        for (int index = 0; index < queue.size(); index++) {
            OrderInput order = queue.get(index);
            Attempt attempt = new Attempt(new StopCandidate(order, input.outletOf(order)),
                    queue.subList(index + 1, queue.size()));
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
        private final Demand pendingDemand;
        private final List<List<VehicleInput>> vehicleTiers;
        private RuleViolation lastBlock;

        Attempt(StopCandidate stop, List<OrderInput> stillToPlace) {
            this.stop = stop;
            this.pendingDemand = demandOfSameTrip(stop, stillToPlace);
            this.vehicleTiers = tiersFor(stop.order());
        }

        Optional<RuleViolation> lastBlock() {
            return Optional.ofNullable(lastBlock);
        }

        boolean placeInto(Map<String, VehicleDay> days) {
            return vehicleTiers.stream().anyMatch(tier -> joinExistingTrip(days, tier) || openNewTrip(days, tier));
        }

        private boolean joinExistingTrip(Map<String, VehicleDay> days, List<VehicleInput> tier) {
            for (VehicleInput vehicle : tier) {
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

        private boolean openNewTrip(Map<String, VehicleDay> days, List<VehicleInput> tier) {
            for (VehicleInput vehicle : sizedForPendingDemand(tier)) {
                VehicleDay day = days.getOrDefault(vehicle.id(), VehicleDay.idle(vehicle));
                VehicleDay candidate = day.withTrip(new TripDraft(List.of(stop)));
                if (accepts(candidate)) {
                    days.put(vehicle.id(), candidate);
                    return true;
                }
            }
            return false;
        }

        /**
         * A new trip should be able to take everything still waiting for the same brand, district and
         * temperature: vehicles that can come first (smallest first), then the rest from largest down,
         * so a busy district does not eat several trips by starting on a small vehicle.
         */
        private List<VehicleInput> sizedForPendingDemand(List<VehicleInput> tier) {
            List<VehicleInput> roomyEnough = tier.stream().filter(pendingDemand::fitsIn).toList();
            List<VehicleInput> tooSmall = new ArrayList<>(tier.stream().filter(vehicle -> !pendingDemand.fitsIn(vehicle)).toList());
            tooSmall.sort(Comparator.comparing(VehicleInput::weightCapKg).thenComparing(VehicleInput::volumeCapM3).reversed()
                    .thenComparing(VehicleInput::id));
            List<VehicleInput> ordered = new ArrayList<>(roomyEnough);
            ordered.addAll(tooSmall);
            return ordered;
        }

        private boolean accepts(VehicleDay candidate) {
            Optional<RuleViolation> violation = checker.firstViolation(candidate);
            violation.ifPresent(found -> lastBlock = found);
            return violation.isEmpty();
        }
    }

    /** Weight and volume of the order being placed plus the orders still waiting for the same kind of trip. */
    private record Demand(BigDecimal weightKg, BigDecimal volumeM3) {

        boolean fitsIn(VehicleInput vehicle) {
            return weightKg.compareTo(vehicle.weightCapKg()) <= 0 && volumeM3.compareTo(vehicle.volumeCapM3()) <= 0;
        }
    }

    private Demand demandOfSameTrip(StopCandidate stop, List<OrderInput> stillToPlace) {
        OrderInput placing = stop.order();
        BigDecimal weight = placing.weightKg();
        BigDecimal volume = placing.volumeM3();
        for (OrderInput other : stillToPlace) {
            boolean sameKindOfTrip = other.brand() == placing.brand() && other.isChilled() == placing.isChilled()
                    && input.outletOf(other).district().equals(stop.outlet().district());
            if (sameKindOfTrip) {
                weight = weight.add(other.weightKg());
                volume = volume.add(other.volumeM3());
            }
        }
        return new Demand(weight, volume);
    }

    /**
     * Vehicles to try, in order: smallest first. An ambient order tries every other vehicle completely
     * before it touches a fridge vehicle, so fridge capacity stays free for chilled orders.
     */
    private List<List<VehicleInput>> tiersFor(OrderInput order) {
        Comparator<VehicleInput> smallestFirst = Comparator
                .comparing(VehicleInput::weightCapKg)
                .thenComparing(VehicleInput::volumeCapM3)
                .thenComparing(VehicleInput::id);
        List<VehicleInput> sorted = input.vehicles().stream().sorted(smallestFirst).toList();
        if (order.isChilled()) {
            return List.of(sorted);
        }
        List<VehicleInput> dry = sorted.stream().filter(vehicle -> !vehicle.isReefer()).toList();
        List<VehicleInput> fridge = sorted.stream().filter(VehicleInput::isReefer).toList();
        return List.of(dry, fridge);
    }
}
