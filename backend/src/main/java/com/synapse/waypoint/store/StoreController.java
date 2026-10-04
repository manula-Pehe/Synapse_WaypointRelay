package com.synapse.waypoint.store;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.synapse.waypoint.common.dto.ListResponse;
import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.CreateOrderRequest;
import com.synapse.waypoint.core.order.dto.CloseStatusDto;
import com.synapse.waypoint.core.order.dto.OrderDetailDto;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.reference.entity.Outlet;
import com.synapse.waypoint.core.reference.repository.OutletRepository;

@RestController
@RequestMapping("/api/store")
class StoreController {
    private final OrderService orders;
    private final CurrentUser user;
    private final DemoClock clock;
    private final OutletRepository outlets;
    private final JdbcTemplate jdbc;
    private final StoreOrderAccess access;
    private final EntityManager entityManager;

    StoreController(OrderService orders, CurrentUser user, DemoClock clock, OutletRepository outlets,
                    JdbcTemplate jdbc, StoreOrderAccess access, EntityManager entityManager) {
        this.orders = orders; this.user = user; this.clock = clock; this.outlets = outlets;
        this.jdbc = jdbc; this.access = access; this.entityManager = entityManager;
    }

    private String outletId() {
        return access.outletId();
    }

    @GetMapping("/home")
    Home home() {
        Outlet outlet = outlets.findById(outletId()).orElseThrow();
        LocalDate runDate = clock.runDate();
        CloseStatusDto close = access.closeStatus(outletId(), runDate);
        Integer issues = jdbc.queryForObject("SELECT count(*) FROM issues WHERE outlet_id = ? AND status <> 'RESOLVED'", Integer.class, outletId());
        return new Home(outlet.getId(), outlet.getBrand(), runDate, clock.now(), close.closed(),
                close.cutOffAt().toString(),
                access.find(runDate, runDate), access.find(clock.today(), clock.today()), issues);
    }

    @GetMapping("/orders")
    ListResponse<OrderDto> list(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        LocalDate start = from == null ? clock.today().minusDays(30) : from;
        LocalDate end = to == null ? clock.today().plusDays(30) : to;
        if (end.isBefore(start) || start.plusYears(2).isBefore(end))
            throw new DomainException(ErrorCode.VALIDATION, "Choose a date range of at most two years.");
        return ListResponse.of(access.find(start, end));
    }

    @GetMapping("/orders/{id}")
    OrderDetailDto detail(@PathVariable String id) { return new OrderDetailDto(orders.get(id), orders.history(id)); }

    @GetMapping("/deliveries")
    ListResponse<DeliveryView> deliveries(@RequestParam(required = false) LocalDate runDate) {
        LocalDate date = runDate == null ? clock.today() : runDate;
        return ListResponse.of(access.find(date, date).stream().map(order -> {
            List<ReceiptView> receipts = jdbc.query("SELECT received_units,received_at FROM receipts WHERE order_id = ?",
                    (rs, row) -> new ReceiptView(rs.getInt(1), rs.getTimestamp(2).toInstant()), order.id());
            return new DeliveryView(order.id(), order.ref(), order.status(), null, null, null, null,
                    receipts.isEmpty() ? null : receipts.get(0));
        }).toList());
    }

    @PutMapping("/orders/{id}")
    OrderDto edit(@PathVariable String id, @Valid @RequestBody Units body) {
        return orders.editUnits(id, body.units());
    }

    @PostMapping("/orders/{id}/confirm")
    OrderDto confirm(@PathVariable String id) {
        return orders.confirm(id);
    }

    @PostMapping("/orders/{id}/cancel")
    OrderDto cancel(@PathVariable String id, @Valid @RequestBody(required = false) CancelReason body) {
        return orders.cancel(id, body == null || body.reason() == null || body.reason().isBlank() ? "Cancelled by store" : body.reason());
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    OrderDto create(@Valid @RequestBody NewStoreOrder body) {
        return orders.createStoreOrder(new CreateOrderRequest(outletId(), body.runDate(), body.temp(), body.units(), body.note()));
    }

    @PostMapping("/orders/{id}/check")
    @Transactional
    OrderDto check(@PathVariable String id, @Valid @RequestBody PhoneCheck body) {
        OrderDto order = orders.get(id);
        if (order.source() != com.synapse.waypoint.core.order.entity.OrderSource.PHONE_IN || order.storeChecked())
            throw new DomainException(ErrorCode.INVALID_STATUS, "This phone order has already been checked.");
        if (!body.ok()) {
            if (body.message() == null || body.message().isBlank())
                throw new DomainException(ErrorCode.VALIDATION, "Tell dispatch what is wrong.");
            jdbc.update("INSERT INTO order_disputes(id,order_id,outlet_id,message,created_by,created_at) VALUES (?,?,?,?,?,?)",
                    UUID.randomUUID().toString(), id, outletId(), body.message().strip(), user.id(), Timestamp.from(clock.now()));
        }
        int changed = jdbc.update("UPDATE orders SET store_checked = true, updated_at = ?, version = version + 1 WHERE id = ? AND store_checked = false",
                Timestamp.from(clock.now()), id);
        if (changed != 1) throw new DomainException(ErrorCode.CONFLICT, "This phone order was checked already.");
        jdbc.update("""
                INSERT INTO order_events(order_id,at,actor_user_id,type,from_status,to_status)
                VALUES (?,?,?,?,?,?)""", id, Timestamp.from(clock.now()), user.id(), body.ok() ? "STORE_CHECKED" : "DISPUTED",
                order.status().name(), order.status().name());
        entityManager.clear();
        return orders.get(id);
    }

    @PostMapping("/orders/{id}/dispute")
    @Transactional
    Map<String, String> dispute(@PathVariable String id, @Valid @RequestBody Dispute body) {
        OrderDto order = orders.get(id);
        if (order.source() != com.synapse.waypoint.core.order.entity.OrderSource.PHONE_IN)
            throw new DomainException(ErrorCode.INVALID_STATUS, "Only phone orders can be disputed here.");
        if (body.message().isBlank()) throw new DomainException(ErrorCode.VALIDATION, "A message is required.");
        String disputeId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO order_disputes(id,order_id,outlet_id,message,created_by,created_at) VALUES (?,?,?,?,?,?)",
                disputeId, order.id(), outletId(), body.message().strip(), user.id(), Timestamp.from(clock.now()));
        return Map.of("id", disputeId, "status", "OPEN");
    }

    @PostMapping("/orders/{id}/receipt")
    @Transactional
    Map<String, Object> receipt(@PathVariable String id, @Valid @RequestBody Receipt body) {
        OrderDto order = orders.get(id);
        if (order.status() != OrderStatus.DELIVERED && order.status() != OrderStatus.PARTIAL)
            throw new DomainException(ErrorCode.INVALID_STATUS, "The order has not been delivered.");
        int deliveredUnits = orders.history(id).stream()
                .filter(event -> event.type().equals("DELIVERED") || event.type().equals("PARTIAL"))
                .reduce((first, last) -> last)
                .map(event -> ((Number) event.details().getOrDefault("units", order.units())).intValue())
                .orElse(order.units());
        if (body.receivedUnits() > deliveredUnits) throw new DomainException(ErrorCode.VALIDATION, "Received units exceed delivered units.");
        Boolean exists = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM receipts WHERE order_id = ?)", Boolean.class, id);
        if (Boolean.TRUE.equals(exists)) throw new DomainException(ErrorCode.DUPLICATE, "Receipt is already confirmed.");
        String receiptId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO receipts(id,order_id,received_units,note,received_by,received_at) VALUES (?,?,?,?,?,?)",
                receiptId, id, body.receivedUnits(), body.note(), user.id(), Timestamp.from(clock.now()));
        return Map.of("id", receiptId, "orderId", id, "receivedUnits", body.receivedUnits(), "at", clock.now());
    }

    record Home(String outlet, String brand, LocalDate runDate, java.time.Instant now, boolean ordersClosed, String cutOffAt,
                List<OrderDto> tomorrow, List<OrderDto> today, int openIssues) {}
    record DeliveryView(String orderId, String orderRef, OrderStatus status, Object arrival,
                        Object deferral, Object delivery, Object shortfall, ReceiptView receipt) {}
    record ReceiptView(int receivedUnits, java.time.Instant at) {}
    record Units(@Min(1) int units) {}
    record CancelReason(@Size(max = 100) String reason) {}
    record NewStoreOrder(@NotNull LocalDate runDate, @NotNull TemperatureRequirement temp, @Min(1) int units,
                         @Size(max = 500) String note) {}
    record PhoneCheck(boolean ok, @Size(max = 1000) String message) {}
    record Dispute(@NotBlank @Size(max = 1000) String message) {}
    record Receipt(@Min(0) int receivedUnits, @Size(max = 1000) String note) {}
}
