package com.synapse.waypoint.core.job;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.core.order.dto.UnconfirmedOutletDto;
import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.service.CutOffSchedule;
import com.synapse.waypoint.core.order.service.UnconfirmedOrderService;
import com.synapse.waypoint.notification.entity.NotificationSeverity;
import com.synapse.waypoint.notification.recipient.NotificationScope;
import com.synapse.waypoint.notification.service.NotificationService;

/** 3:30 PM on the day before the run: tells the depot's dispatchers which stores have not confirmed (D1u). */
@Component
class DispatcherAlertJob extends PreCutOffJob {

    static final String NAME = "dispatcher-alert";
    static final String NOTIFICATION_TYPE = "UNCONFIRMED_ORDERS";
    static final String UNCONFIRMED_LINK = "/dispatch";

    private static final Duration LEAD_TIME = Duration.ofMinutes(30);
    private static final int MAX_STORES_NAMED = 8;

    private final NotificationService notifications;

    DispatcherAlertJob(CutOffSchedule cutOff, CloseOrdersService closeOrders,
            UnconfirmedOrderService unconfirmedOrders, NotificationService notifications) {
        super(cutOff, closeOrders, unconfirmedOrders);
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
        return unconfirmed;
    }

    @Override
    protected void notifyAbout(JobRun run, List<UnconfirmedOutletDto> stores) {
        notifications.notifyRole(Role.DISPATCHER, NotificationScope.depot(run.depot()), NotificationSeverity.WARNING,
                NOTIFICATION_TYPE, titleFor(run.depot(), stores.size()), bodyFor(stores), UNCONFIRMED_LINK);
    }

    private static String titleFor(String depot, int storeCount) {
        String stores = storeCount == 1 ? "1 store has" : storeCount + " stores have";
        return depot + ": " + stores + " not confirmed";
    }

    private static String bodyFor(List<UnconfirmedOutletDto> stores) {
        List<String> named = stores.stream().limit(MAX_STORES_NAMED).map(DispatcherAlertJob::label).toList();
        String list = String.join(", ", named);
        int others = stores.size() - named.size();
        return others > 0 ? list + " and " + others + " more. Orders close at 4 PM." : list + ". Orders close at 4 PM.";
    }

    private static String label(UnconfirmedOutletDto store) {
        return store.outletName() + " (" + store.outletId() + ")";
    }
}
