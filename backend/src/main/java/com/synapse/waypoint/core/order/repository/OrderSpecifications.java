package com.synapse.waypoint.core.order.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.synapse.waypoint.core.order.dto.OrderFilters;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.reference.entity.Outlet;

/** Builds the order list query from a run date, an optional depot and optional filters. */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> forRun(LocalDate runDate, String depot, OrderFilters filters) {
        List<Specification<Order>> parts = new ArrayList<>();
        parts.add((order, query, cb) -> cb.equal(order.get("runDate"), runDate));
        if (depot != null && !depot.isBlank()) {
            parts.add(inDepot(depot));
        }
        if (filters.status() != null) {
            parts.add((order, query, cb) -> cb.equal(order.get("status"), filters.status()));
        }
        if (filters.outletId() != null) {
            parts.add((order, query, cb) -> cb.equal(order.get("outletId"), filters.outletId()));
        }
        if (filters.brand() != null) {
            parts.add((order, query, cb) -> cb.equal(cb.lower(order.get("brand")), filters.brand().toLowerCase()));
        }
        if (filters.temp() != null) {
            parts.add((order, query, cb) -> cb.equal(order.get("temperatureRequirement"), filters.temp()));
        }
        return Specification.allOf(parts);
    }

    private static Specification<Order> inDepot(String depot) {
        return (order, query, cb) -> {
            var outletsInDepot = query.subquery(String.class);
            var outlet = outletsInDepot.from(Outlet.class);
            outletsInDepot.select(outlet.get("id"))
                    .where(cb.equal(cb.lower(outlet.get("depot")), depot.strip().toLowerCase()));
            return order.get("outletId").in(outletsInDepot);
        };
    }
}
