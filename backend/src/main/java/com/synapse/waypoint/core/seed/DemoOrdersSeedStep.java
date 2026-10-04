package com.synapse.waypoint.core.seed;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.seed.DemoDay;
import com.synapse.waypoint.common.seed.SeedStep;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.entity.NewOrder;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderEvent;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderEventRepository;
import com.synapse.waypoint.core.order.repository.OrderRepository;

/**
 * Loads the demo day's orders. The demo store's orders start {@code PREPARED} so its manager can
 * confirm them; all others start {@code CONFIRMED}. Each order gets one history row.
 */
@Component
class DemoOrdersSeedStep implements SeedStep {

    static final int ORDER = 30;
    static final String ORDERS_FILE = "task2b_peak_day_scenarios.csv";

    private final DatasetLocator locator;
    private final CsvReader reader;
    private final OrderRepository orders;
    private final OrderEventRepository events;
    private final DemoClock clock;

    DemoOrdersSeedStep(DatasetLocator locator, CsvReader reader, OrderRepository orders, OrderEventRepository events,
            DemoClock clock) {
        this.locator = locator;
        this.reader = reader;
        this.orders = orders;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public String name() {
        return "demo day orders";
    }

    @Override
    public void run() {
        LocalDate runDate = clock.runDate();
        Instant now = clock.now();
        reader.read(locator.locate(ORDERS_FILE)).stream()
                .filter(row -> DemoDay.SCENARIO.equals(row.text("scenario")))
                .forEach(row -> seed(row, runDate, now));
    }

    private void seed(CsvRow row, LocalDate runDate, Instant now) {
        Order order = Order.create(newOrder(row, runDate), now);
        orders.save(order);
        events.save(new OrderEvent(order.getId(), now, null, order.getStatus().name(), null, order.getStatus(),
                Map.of()));
    }

    private NewOrder newOrder(CsvRow row, LocalDate runDate) {
        String outletId = row.text("outlet_id");
        return new NewOrder(UUID.randomUUID().toString(), row.text("order_ref"), outletId, row.text("brand"),
                temperature(row), row.integer("order_units"), row.decimal("order_weight_kg"),
                row.decimal("order_volume_m3"), runDate, startingStatus(outletId), OrderSource.SEED, false,
                row.integer("days_since_last_served"), row.flag("deferred_yesterday"), null, true);
    }

    private OrderStatus startingStatus(String outletId) {
        return DemoDay.STORE_OUTLET_ID.equals(outletId) ? OrderStatus.PREPARED : OrderStatus.CONFIRMED;
    }

    private TemperatureRequirement temperature(CsvRow row) {
        return switch (row.text("temp_requirement").toLowerCase()) {
            case "chilled" -> TemperatureRequirement.CHILLED;
            case "ambient" -> TemperatureRequirement.AMBIENT;
            default -> throw row.invalid("temp_requirement",
                    "must be chilled or ambient: '" + row.text("temp_requirement") + "'");
        };
    }
}
