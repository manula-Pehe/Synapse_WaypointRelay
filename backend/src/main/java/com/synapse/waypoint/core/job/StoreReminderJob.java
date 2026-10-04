package com.synapse.waypoint.core.job;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.UnconfirmedOutletDto;
import com.synapse.waypoint.core.order.service.AutoConfirmPolicy;
import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.service.CutOffSchedule;
import com.synapse.waypoint.core.order.service.UnconfirmedOrderService;
import com.synapse.waypoint.notification.entity.NotificationSeverity;
import com.synapse.waypoint.notification.recipient.NotificationScope;
import com.synapse.waypoint.notification.service.NotificationService;

/**
 * 3 PM on the day before the run: reminds each store that still has chilled, Style or Tech orders
 * to confirm. Fresh ambient orders are confirmed automatically at the cut-off, so they are not chased.
 */
@Component
class StoreReminderJob extends PreCutOffJob {

    static final String NAME = "store-reminder";
    static final String NOTIFICATION_TYPE = "CUTOFF_REMINDER";
    static final String STORE_ORDERS_LINK = "/store/orders";

    private static final Duration LEAD_TIME = Duration.ofHours(1);
    private static final DateTimeFormatter RUN_DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    private final AutoConfirmPolicy autoConfirmPolicy;
    private final NotificationService notifications;

    StoreReminderJob(CutOffSchedule cutOff, CloseOrdersService closeOrders, UnconfirmedOrderService unconfirmedOrders,
            AutoConfirmPolicy autoConfirmPolicy, NotificationService notifications) {
        super(cutOff, closeOrders, unconfirmedOrders);
        this.autoConfirmPolicy = autoConfirmPolicy;
        this.notifications = notifications;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected Duration leadTime() {
        return LEAD_TIME;
    }

    @Override
    protected List<UnconfirmedOutletDto> storesToChase(List<UnconfirmedOutletDto> unconfirmed) {
        return unconfirmed.stream()
                .map(store -> new UnconfirmedOutletDto(store.outletId(), store.outletName(), store.phone(),
                        store.orders().stream().filter(this::needsStoreConfirmation).toList()))
                .filter(store -> !store.orders().isEmpty())
                .toList();
    }

    @Override
    protected void notifyAbout(JobRun run, List<UnconfirmedOutletDto> stores) {
        stores.forEach(store -> notifications.notifyRole(Role.STORE_MANAGER, NotificationScope.outlet(store.outletId()),
                NotificationSeverity.WARNING, NOTIFICATION_TYPE, "Confirm by 4 PM", bodyFor(run.runDate(), store),
                STORE_ORDERS_LINK));
    }

    private boolean needsStoreConfirmation(OrderDto order) {
        return !autoConfirmPolicy.appliesTo(order.brand(), order.temp());
    }

    private static String bodyFor(LocalDate runDate, UnconfirmedOutletDto store) {
        int count = store.orders().size();
        String orders = count == 1 ? "1 order" : count + " orders";
        return "You have " + orders + " for " + RUN_DAY.format(runDate) + " not confirmed yet. "
                + "Orders close at 4 PM; anything unconfirmed is left out of the run.";
    }
}
