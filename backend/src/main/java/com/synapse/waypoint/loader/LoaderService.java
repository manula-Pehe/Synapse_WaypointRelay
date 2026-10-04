package com.synapse.waypoint.loader;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.auth.entity.UserAccount;
import com.synapse.waypoint.auth.repository.UserAccountRepository;
import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.common.time.ApiTimestamp;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.reference.entity.Outlet;
import com.synapse.waypoint.core.reference.repository.OutletRepository;
import com.synapse.waypoint.notification.entity.NotificationSeverity;
import com.synapse.waypoint.notification.recipient.NotificationScope;
import com.synapse.waypoint.notification.service.NotificationService;
import com.synapse.waypoint.planning.dto.PlanDto;
import com.synapse.waypoint.planning.dto.PlanStopDto;
import com.synapse.waypoint.planning.dto.PlanTripDto;
import com.synapse.waypoint.planning.dto.PlanVehicleDto;
import com.synapse.waypoint.planning.service.PlanQueryService;

@Service
@Transactional
public class LoaderService implements LoadingQueryService {
    public record TripSummary(String tripId, String vehicleId, int tripNo, String district, String brand,
            int stops, int units, boolean chilled, OffsetDateTime departAt, String status, int ticked,
            boolean vehicleAvailable) {}
    public record TripList(List<TripSummary> items, int total, OffsetDateTime listsAvailableAt) {}
    public record StopDetail(String stopId, int loadSeq, String orderId, String orderRef, String outletId,
            String outletName, int units, BigDecimal weightKg, BigDecimal volumeM3, String accessNote,
            String storeNote, boolean ticked, int missingUnits) {}
    public record FridgeCheck(boolean running, BigDecimal tempC, boolean doorsOk, boolean passed,
            OffsetDateTime checkedAt) {}
    public record TripDetail(TripSummary trip, String vehicleType, BigDecimal weightCapKg,
            BigDecimal volumeCapM3, BigDecimal loadedWeightKg, BigDecimal loadedVolumeM3,
            List<StopDetail> stops, FridgeCheck fridgeCheck) {}
    public record ShortfallResult(String orderId, String remainderOrderId, String remainderOrderRef,
            int missingUnits, String reason, String note) {}
    public record HandoverResult(String status, OffsetDateTime at) {}

    private final JdbcTemplate jdbc;
    private final PlanQueryService plans;
    private final OrderService orders;
    private final OutletRepository outlets;
    private final UserAccountRepository users;
    private final NotificationService notifications;
    private final CurrentUser current;
    private final DemoClock clock;

    public LoaderService(JdbcTemplate jdbc, PlanQueryService plans, OrderService orders,
            OutletRepository outlets, UserAccountRepository users, NotificationService notifications,
            CurrentUser current, DemoClock clock) {
        this.jdbc = jdbc;
        this.plans = plans;
        this.orders = orders;
        this.outlets = outlets;
        this.users = users;
        this.notifications = notifications;
        this.current = current;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TripList trips(LocalDate runDate) {
        String depot = depot();
        List<TripSummary> items = loadingStatus(runDate, depot);
        return new TripList(items, items.size(), runDate.atTime(LocalTime.of(3, 30))
                .atZone(DemoClock.ZONE).toOffsetDateTime());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripSummary> loadingStatus(LocalDate runDate, String depot) {
        Optional<PlanDto> plan = plans.publishedPlan(runDate, depot);
        if (plan.isEmpty()) return List.of();
        List<TripSummary> result = new ArrayList<>();
        for (PlanVehicleDto vehicle : plan.get().vehicles()) {
            for (PlanTripDto trip : vehicle.trips()) result.add(summary(vehicle, trip));
        }
        result.sort(Comparator.comparing(TripSummary::departAt)
                .thenComparing(TripSummary::chilled, Comparator.reverseOrder())
                .thenComparing(TripSummary::vehicleId));
        return List.copyOf(result);
    }

    @Transactional(readOnly = true)
    public TripDetail trip(String tripId) {
        Placement placement = placement(tripId);
        List<StopDetail> stops = placement.trip.stops().stream()
                .sorted(Comparator.comparingInt(PlanStopDto::loadSeq))
                .map(this::stopDetail).toList();
        BigDecimal weight = stops.stream().map(StopDetail::weightKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal volume = stops.stream().map(StopDetail::volumeM3).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TripDetail(summary(placement.vehicle, placement.trip), placement.vehicle.type(),
                placement.vehicle.weightCapKg(), placement.vehicle.volumeCapM3(), weight, volume,
                stops, latestCheck(tripId).orElse(null));
    }

    public FridgeCheck fridgeCheck(String tripId, boolean running, BigDecimal tempC, boolean doorsOk) {
        lockTrip(tripId);
        TripDetail detail = trip(tripId);
        requireBeforeHandover(detail.trip());
        requireVehicleAvailable(detail.trip());
        if (!detail.trip().chilled()) throw new DomainException(ErrorCode.VALIDATION, "This trip has no chilled goods.");
        if (tempC == null || tempC.scale() > 1 || tempC.compareTo(new BigDecimal("-99.9")) < 0 ||
                tempC.compareTo(new BigDecimal("99.9")) > 0)
            throw new DomainException(ErrorCode.VALIDATION, "Enter a valid fridge temperature.");
        boolean passed = running && doorsOk && tempC.compareTo(BigDecimal.ZERO) >= 0 &&
                tempC.compareTo(new BigDecimal("5.0")) <= 0;
        jdbc.update("INSERT INTO fridge_checks(id,trip_id,running,temp_c,doors_ok,passed,checked_by,checked_at) VALUES (?,?,?,?,?,?,?,?)",
                id(), tripId, running, tempC, doorsOk, passed, current.id(), sqlNow());
        if (!passed) notifications.notifyRole(Role.DISPATCHER, NotificationScope.depot(depot()),
                NotificationSeverity.CRITICAL, "FRIDGE_CHECK_FAILED", "Fridge check failed",
                detail.trip().vehicleId() + " · Trip " + detail.trip().tripNo() + " is blocked for chilled loading.",
                "/dispatch");
        return new FridgeCheck(running, tempC, doorsOk, passed, ApiTimestamp.of(clock.now()));
    }

    public TripDetail tick(String stopId) {
        String tripId = tripIdForStop(stopId);
        lockTrip(tripId);
        TripDetail detail = trip(tripId);
        requireBeforeHandover(detail.trip());
        requireVehicleAvailable(detail.trip());
        if (detail.stops().stream().noneMatch(s -> s.stopId().equals(stopId))) throw new NotFoundException("stop", stopId);
        if (detail.trip().chilled() && (detail.fridgeCheck() == null || !detail.fridgeCheck().passed()))
            throw new DomainException(ErrorCode.RULE_VIOLATION, "Pass the fridge check before loading chilled goods.");
        jdbc.update("INSERT INTO load_ticks(stop_id,ticked_by,ticked_at) VALUES (?,?,?) ON CONFLICT (stop_id) DO NOTHING",
                stopId, current.id(), sqlNow());
        refreshStatus(tripId, detail.stops().size());
        return trip(tripId);
    }

    public ShortfallResult shortfall(String stopId, int missingUnits, String reason, String note) {
        String tripId = tripIdForStop(stopId);
        lockTrip(tripId);
        TripDetail detail = trip(tripId);
        requireBeforeHandover(detail.trip());
        requireVehicleAvailable(detail.trip());
        StopDetail stop = detail.stops().stream().filter(s -> s.stopId().equals(stopId))
                .findFirst().orElseThrow(() -> new NotFoundException("stop", stopId));
        if (stop.missingUnits() > 0) throw new DomainException(ErrorCode.DUPLICATE, "A shortfall is already recorded for this stop.");
        if (missingUnits <= 0 || missingUnits >= stop.units())
            throw new DomainException(ErrorCode.VALIDATION,
                    "At least one case must be loaded. Contact dispatch if the entire stop is short.");
        if (reason == null || !List.of("MISSING", "DAMAGED", "WRONG_ITEM").contains(reason))
            throw new DomainException(ErrorCode.VALIDATION, "Choose a shortfall reason.");
        OrderDto remainder = orders.createRemainder(stop.orderId(), missingUnits, reason +
                (note == null || note.isBlank() ? "" : ": " + note.strip()));
        jdbc.update("INSERT INTO shortfalls(id,stop_id,order_id,missing_units,reason,note,remainder_order_id,reported_by,reported_at) VALUES (?,?,?,?,?,?,?,?,?)",
                id(), stopId, stop.orderId(), missingUnits, reason, note, remainder.id(), current.id(), sqlNow());
        String message = missingUnits + " cases short for " + stop.orderRef() + ". Remainder " + remainder.ref() + " booked.";
        notifications.notifyRole(Role.STORE_MANAGER, NotificationScope.outlet(stop.outletId()),
                NotificationSeverity.WARNING, "LOAD_SHORTFALL", "Loading shortfall", message, "/store");
        notifications.notifyRole(Role.DISPATCHER, NotificationScope.depot(depot()),
                NotificationSeverity.WARNING, "LOAD_SHORTFALL", "Loading shortfall", message, "/dispatch");
        return new ShortfallResult(stop.orderId(), remainder.id(), remainder.ref(), missingUnits, reason, note);
    }

    public HandoverResult handover(String tripId, String driverStaffId) {
        lockTrip(tripId);
        TripDetail detail = trip(tripId);
        requireBeforeHandover(detail.trip());
        requireVehicleAvailable(detail.trip());
        if (!"READY".equals(detail.trip().status()))
            throw new DomainException(ErrorCode.INVALID_STATUS, "Tick every stop before handover.");
        if (detail.trip().chilled() && (detail.fridgeCheck() == null || !detail.fridgeCheck().passed()))
            throw new DomainException(ErrorCode.RULE_VIOLATION, "The fridge check must pass before handover.");
        if (detail.loadedWeightKg().compareTo(detail.weightCapKg()) > 0 ||
                detail.loadedVolumeM3().compareTo(detail.volumeCapM3()) > 0)
            throw new DomainException(ErrorCode.RULE_VIOLATION, "The load exceeds vehicle capacity.");
        UserAccount driver = users.findByStaffIdIgnoreCaseAndActiveTrue(driverStaffId == null ? "" : driverStaffId.strip())
                .filter(user -> user.getRole() == Role.DRIVER && detail.trip().vehicleId().equals(user.getVehicleId()))
                .orElseThrow(() -> new DomainException(ErrorCode.VALIDATION, "Enter the assigned driver's staff ID."));
        for (StopDetail stop : detail.stops()) orders.markLoaded(stop.orderId());
        jdbc.update("INSERT INTO handovers(id,trip_id,driver_id,loader_id,cases,weight_kg,at) VALUES (?,?,?,?,?,?,?)",
                id(), tripId, driver.getId(), current.id(), detail.stops().stream().mapToInt(StopDetail::units).sum(),
                detail.loadedWeightKg(), sqlNow());
        jdbc.update("UPDATE trip_loading SET status='LOADED',loaded_by=?,loaded_at=?,updated_at=? WHERE trip_id=?",
                current.id(), sqlNow(), sqlNow(), tripId);
        return new HandoverResult("LOADED", ApiTimestamp.of(clock.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShortfallResult> shortfallForOrder(String orderId) {
        return jdbc.query("SELECT order_id,remainder_order_id,missing_units,reason,note FROM shortfalls WHERE order_id=? ORDER BY reported_at DESC LIMIT 1",
                (rs, row) -> new ShortfallResult(rs.getString(1), rs.getString(2),
                        orders.get(rs.getString(2)).ref(), rs.getInt(3), rs.getString(4), rs.getString(5)), orderId)
                .stream().findFirst();
    }

    private TripSummary summary(PlanVehicleDto vehicle, PlanTripDto trip) {
        int units = trip.stops().stream().mapToInt(PlanStopDto::units).sum();
        int ticked = jdbc.queryForObject("SELECT count(*) FROM load_ticks lt JOIN stops s ON s.id=lt.stop_id WHERE s.trip_id=?",
                Integer.class, trip.id());
        String status = jdbc.query("SELECT status FROM trip_loading WHERE trip_id=?",
                (rs, row) -> rs.getString(1), trip.id()).stream().findFirst()
                .orElse(ticked == trip.stops().size() && ticked > 0 ? "READY" : ticked > 0 ? "LOADING" : "WAITING");
        if ("LOADED".equals(status) && trip.stops().stream().allMatch(stop -> {
            var orderStatus = orders.get(stop.orderId()).status();
            return orderStatus == com.synapse.waypoint.core.order.entity.OrderStatus.ON_THE_WAY
                    || orderStatus == com.synapse.waypoint.core.order.entity.OrderStatus.DELIVERED
                    || orderStatus == com.synapse.waypoint.core.order.entity.OrderStatus.PARTIAL
                    || orderStatus == com.synapse.waypoint.core.order.entity.OrderStatus.FAILED;
        })) status = "DEPARTED";
        List<PlanTripDto> previousTrips = vehicle.trips().stream()
                .filter(previous -> previous.tripNo() < trip.tripNo()).toList();
        boolean vehicleAvailable = trip.tripNo() == 1 || (!previousTrips.isEmpty() && previousTrips.stream()
                .allMatch(previous -> "DEPARTED".equals(summary(vehicle, previous).status())));
        return new TripSummary(trip.id(), vehicle.vehicleId(), trip.tripNo(), trip.district(), trip.brand(),
                trip.stops().size(), units, trip.stops().stream().anyMatch(s -> "CHILLED".equalsIgnoreCase(s.temp())),
                trip.departAt(), status, ticked, vehicleAvailable);
    }

    private StopDetail stopDetail(PlanStopDto stop) {
        OrderDto order = orders.get(stop.orderId());
        Outlet outlet = outlets.findById(stop.outletId()).orElseThrow(() -> new NotFoundException("outlet", stop.outletId()));
        int missing = jdbc.queryForObject("SELECT coalesce(sum(missing_units),0) FROM shortfalls WHERE stop_id=?",
                Integer.class, stop.id());
        int loaded = stop.units() - missing;
        BigDecimal fraction = stop.units() == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(loaded).divide(BigDecimal.valueOf(stop.units()), 8, java.math.RoundingMode.HALF_UP);
        boolean ticked = !jdbc.query("SELECT stop_id FROM load_ticks WHERE stop_id=?",
                (rs, row) -> rs.getString(1), stop.id()).isEmpty();
        String dock = outlet.getDockType().replace('_', ' ').toLowerCase();
        String parking = outlet.getParkingConstraint().replace('_', ' ').toLowerCase();
        String access = "normal".equals(parking) ? dock : parking + " · " + dock;
        String storeNote = order.source() == OrderSource.STORE ? orders.history(stop.orderId()).stream()
                .map(event -> event.details().get("note"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(note -> !note.isBlank())
                .findFirst().orElse(null) : null;
        return new StopDetail(stop.id(), stop.loadSeq(), stop.orderId(), stop.orderRef(), stop.outletId(),
                order.outletName(), loaded, order.weightKg().multiply(fraction),
                order.volumeM3().multiply(fraction), access, storeNote, ticked, missing);
    }

    private Optional<FridgeCheck> latestCheck(String tripId) {
        return jdbc.query("SELECT running,temp_c,doors_ok,passed,checked_at FROM fridge_checks WHERE trip_id=? ORDER BY recorded_seq DESC LIMIT 1",
                (rs, row) -> new FridgeCheck(rs.getBoolean(1), rs.getBigDecimal(2), rs.getBoolean(3),
                        rs.getBoolean(4), ApiTimestamp.of(rs.getTimestamp(5).toInstant())), tripId)
                .stream().findFirst();
    }

    private Placement placement(String tripId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT p.run_date,p.depot FROM trips t JOIN plans p ON p.id=t.plan_id WHERE t.id=? AND p.status='PUBLISHED'", tripId);
        if (rows.isEmpty()) throw new NotFoundException("trip", tripId);
        Map<String, Object> row = rows.getFirst();
        String ownerDepot = (String) row.get("depot");
        if (!ownerDepot.equalsIgnoreCase(depot())) throw new NotFoundException("trip", tripId);
        LocalDate runDate = ((java.sql.Date) row.get("run_date")).toLocalDate();
        PlanDto plan = plans.publishedPlan(runDate, ownerDepot).orElseThrow(() -> new NotFoundException("trip", tripId));
        for (PlanVehicleDto vehicle : plan.vehicles())
            for (PlanTripDto trip : vehicle.trips())
                if (trip.id().equals(tripId)) return new Placement(vehicle, trip);
        throw new NotFoundException("trip", tripId);
    }

    private String tripIdForStop(String stopId) {
        return jdbc.query("SELECT trip_id FROM stops WHERE id=?", (rs, row) -> rs.getString(1), stopId)
                .stream().findFirst().orElseThrow(() -> new NotFoundException("stop", stopId));
    }

    private void lockTrip(String tripId) {
        placement(tripId);
        jdbc.queryForList("SELECT id FROM trips WHERE id=? FOR UPDATE", tripId);
    }

    private void requireVehicleAvailable(TripSummary trip) {
        if (!trip.vehicleAvailable())
            throw new DomainException(ErrorCode.RULE_VIOLATION, "This vehicle must finish its earlier trip first.");
    }

    private void refreshStatus(String tripId, int stops) {
        int ticked = jdbc.queryForObject("SELECT count(*) FROM load_ticks lt JOIN stops s ON s.id=lt.stop_id WHERE s.trip_id=?",
                Integer.class, tripId);
        String status = ticked == stops ? "READY" : "LOADING";
        jdbc.update("INSERT INTO trip_loading(trip_id,status,updated_at) VALUES (?,?,?) ON CONFLICT(trip_id) DO UPDATE SET status=EXCLUDED.status,updated_at=EXCLUDED.updated_at",
                tripId, status, sqlNow());
    }

    private void requireBeforeHandover(TripSummary trip) {
        if (List.of("LOADED", "DEPARTED").contains(trip.status()))
            throw new DomainException(ErrorCode.INVALID_STATUS, "This trip was already handed over.");
    }

    private String depot() {
        return current.depot().orElseThrow(() -> new DomainException(ErrorCode.FORBIDDEN, "Loader depot is missing."));
    }

    private static String id() { return UUID.randomUUID().toString(); }
    private java.sql.Timestamp sqlNow() { return java.sql.Timestamp.from(clock.now()); }
    private record Placement(PlanVehicleDto vehicle, PlanTripDto trip) {}
}
