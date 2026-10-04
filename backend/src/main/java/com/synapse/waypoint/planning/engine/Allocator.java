package com.synapse.waypoint.planning.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import com.synapse.waypoint.planning.engine.input.OrderInput;
import com.synapse.waypoint.planning.engine.input.PlanningInput;
import com.synapse.waypoint.planning.engine.input.VehicleInput;
import com.synapse.waypoint.planning.engine.rule.RuleChecker;
import com.synapse.waypoint.planning.engine.rule.RuleViolation;

/**
 * One greedy run. Orders go in most constrained first (fewest vehicles that could carry them), then by
 * priority, then larger first. Each order joins an existing compatible trip if one has room, otherwise
 * opens a new trip on a vehicle chosen by the {@link PlacementStrategy}, keeping fridge vehicles free for
 * chilled orders where possible. Every candidate is accepted only if the {@link RuleChecker} finds no
 * violation for the vehicle's whole day. The same strategy always gives the same result.
 */
public class Allocator {

    private static final Comparator<StopCandidate> BY_WINDOW_CLOSE = Comparator
            .comparing((StopCandidate stop) -> stop.window().close())
            .thenComparing(stop -> stop.window().open())
            .thenComparing(stop -> stop.order().ref());

    private final PlanningInput input;
    private final PriorityScorer scorer;
    private final RuleChecker checker;
    private final Map<String, Integer> carriersByOrderId;
    private final VehicleTiers smallestFirst;
    private final VehicleTiers largestFirst;

    public Allocator(PlanningInput input, PriorityScorer scorer, RuleChecker checker) {
        this.input = input;
        this.scorer = scorer;
        this.checker = checker;
        CarrierCounter counter = new CarrierCounter(input);
        this.carriersByOrderId = input.orders().stream()
                .collect(Collectors.toUnmodifiableMap(OrderInput::id, counter::countFor));
        Comparator<VehicleInput> bySize = Comparator
                .comparing(VehicleInput::weightCapKg)
                .thenComparing(VehicleInput::volumeCapM3)
                .thenComparing(VehicleInput::id);
        this.smallestFirst = new VehicleTiers(input.vehicles().stream().sorted(bySize).toList());
        this.largestFirst = new VehicleTiers(input.vehicles().stream().sorted(bySize.reversed()).toList());
    }

    public AllocationResult allocate() {
        return allocate(PlacementStrategy.plain());
    }

    public AllocationResult allocate(PlacementStrategy strategy) {
        Map<String, VehicleDay> days = new LinkedHashMap<>();
        List<UnplacedOrder> unplaced = new ArrayList<>();
        List<OrderInput> queue = ordersByPlacementOrder(strategy);
        for (int index = 0; index < queue.size(); index++) {
            OrderInput order = queue.get(index);
            Attempt attempt = new Attempt(new StopCandidate(order, input.outletOf(order)),
                    queue.subList(index + 1, queue.size()), strategy.vehicleChoice());
            if (!attempt.placeInto(days)) {
                unplaced.add(new UnplacedOrder(order, attempt.lastBlock()));
            }
        }
        List<VehicleDay> used = days.values().stream().filter(day -> !day.trips().isEmpty()).toList();
        return new AllocationResult(used, unplaced);
    }

    private List<OrderInput> ordersByPlacementOrder(PlacementStrategy strategy) {
        Comparator<OrderInput> mostConstrainedFirst = Comparator
                .comparingInt((OrderInput order) -> carriersByOrderId.get(order.id()))
                .thenComparing(Comparator.comparingInt(scorer::score).reversed());
        Comparator<OrderInput> plainOrder = mostConstrainedFirst
                .thenComparing(Comparator.comparing(OrderInput::volumeM3).reversed())
                .thenComparing(Comparator.comparing(OrderInput::weightKg).reversed())
                .thenComparing(OrderInput::ref);
        if (strategy.tieBreaker().isEmpty()) {
            return input.orders().stream().sorted(plainOrder).toList();
        }
        return shuffledWithinTies(input.orders().stream().sorted(plainOrder).toList(), mostConstrainedFirst,
                strategy.tieBreaker().get());
    }

    private static List<OrderInput> shuffledWithinTies(List<OrderInput> sorted, Comparator<OrderInput> tieKey,
            Random random) {
        List<OrderInput> result = new ArrayList<>(sorted.size());
        int groupStart = 0;
        while (groupStart < sorted.size()) {
            int groupEnd = groupStart + 1;
            while (groupEnd < sorted.size() && tieKey.compare(sorted.get(groupStart), sorted.get(groupEnd)) == 0) {
                groupEnd++;
            }
            List<OrderInput> group = new ArrayList<>(sorted.subList(groupStart, groupEnd));
            Collections.shuffle(group, random);
            result.addAll(group);
            groupStart = groupEnd;
        }
        return result;
    }

    /** The search for a home for one order; remembers the last rule that said no. */
    private final class Attempt {

        private final StopCandidate stop;
        private final Demand pendingDemand;
        private final List<List<VehicleInput>> joinTiers;
        private final List<List<VehicleInput>> newTripTiers;
        private final boolean sizeForPendingDemand;
        private RuleViolation lastBlock;

        Attempt(StopCandidate stop, List<OrderInput> stillToPlace, NewTripVehicleChoice vehicleChoice) {
            this.stop = stop;
            this.pendingDemand = demandOfSameTrip(stop, stillToPlace);
            this.joinTiers = smallestFirst.forOrder(stop.order());
            this.sizeForPendingDemand = vehicleChoice == NewTripVehicleChoice.SMALLEST_THAT_FITS;
            this.newTripTiers = sizeForPendingDemand ? joinTiers : largestFirst.forOrder(stop.order());
        }

        Optional<RuleViolation> lastBlock() {
            return Optional.ofNullable(lastBlock);
        }

        boolean placeInto(Map<String, VehicleDay> days) {
            for (int tierIndex = 0; tierIndex < joinTiers.size(); tierIndex++) {
                if (joinExistingTrip(days, joinTiers.get(tierIndex)) || openNewTrip(days, newTripTiers.get(tierIndex))) {
                    return true;
                }
            }
            return false;
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
            List<VehicleInput> candidates = sizeForPendingDemand ? sizedForPendingDemand(tier) : tier;
            for (VehicleInput vehicle : candidates) {
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
     * Vehicles to try, in the given size order. An ambient order tries every other vehicle completely
     * before it touches a fridge vehicle, so fridge capacity stays free for chilled orders.
     */
    private static final class VehicleTiers {

        private final List<List<VehicleInput>> forChilled;
        private final List<List<VehicleInput>> forAmbient;

        VehicleTiers(List<VehicleInput> sorted) {
            this.forChilled = List.of(sorted);
            this.forAmbient = List.of(
                    sorted.stream().filter(vehicle -> !vehicle.isReefer()).toList(),
                    sorted.stream().filter(VehicleInput::isReefer).toList());
        }

        List<List<VehicleInput>> forOrder(OrderInput order) {
            return order.isChilled() ? forChilled : forAmbient;
        }
    }
}
