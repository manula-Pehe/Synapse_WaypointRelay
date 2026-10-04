package com.synapse.waypoint.store;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.core.order.dto.CloseStatusDto;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderFilters;
import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.reference.entity.Outlet;
import com.synapse.waypoint.core.reference.repository.OutletRepository;

/** Store-only listing and access to the shared close status, leaving the order module untouched. */
@Component
class StoreOrderAccess {
    private final OrderService orders;
    private final CurrentUser user;
    private final JdbcTemplate jdbc;
    private final OutletRepository outlets;
    private final CloseOrdersService closeOrders;

    StoreOrderAccess(OrderService orders, CurrentUser user, JdbcTemplate jdbc,
                     OutletRepository outlets, CloseOrdersService closeOrders) {
        this.orders = orders; this.user = user; this.jdbc = jdbc;
        this.outlets = outlets; this.closeOrders = closeOrders;
    }

    String outletId() {
        return user.outletId().orElseThrow(
                () -> new DomainException(ErrorCode.FORBIDDEN, "Your account is not linked to an outlet."));
    }

    List<OrderDto> find(LocalDate from, LocalDate to) {
        String outlet = outletId();
        List<LocalDate> dates = jdbc.queryForList("""
                SELECT DISTINCT run_date FROM orders WHERE outlet_id = ? AND run_date BETWEEN ? AND ?
                ORDER BY run_date DESC""", LocalDate.class, outlet, from, to);
        return dates.stream().flatMap(date -> orders.findByRun(date, null,
                new OrderFilters(null, outlet, null, null)).stream()).toList();
    }

    CloseStatusDto closeStatus(String outletId, LocalDate runDate) {
        Outlet outlet = outlets.findById(outletId).orElseThrow();
        return closeOrders.status(runDate, outlet.getDepot());
    }
}
