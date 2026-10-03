package com.synapse.waypoint.core.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.core.order.entity.OrderEvent;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    List<OrderEvent> findByOrderIdOrderByAtAscIdAsc(String orderId);
}
