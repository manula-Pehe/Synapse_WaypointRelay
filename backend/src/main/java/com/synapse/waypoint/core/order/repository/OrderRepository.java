package com.synapse.waypoint.core.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.core.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, String> {
}
