package com.synapse.waypoint.store;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** The receipts the store confirmed, for a set of orders in one query. */
@Component
class StoreReceipts {

    private final JdbcClient jdbc;

    StoreReceipts(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Map<String, ReceiptView> forOrders(Collection<String> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        Map<String, ReceiptView> receipts = new HashMap<>();
        jdbc.sql("SELECT order_id, received_units, received_at FROM receipts WHERE order_id IN (:ids)")
                .param("ids", orderIds)
                .query((rs, row) -> receipts.put(rs.getString("order_id"),
                        new ReceiptView(rs.getInt("received_units"), rs.getTimestamp("received_at").toInstant())))
                .list();
        return Collections.unmodifiableMap(receipts);
    }
}
