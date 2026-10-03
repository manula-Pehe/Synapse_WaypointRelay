package com.synapse.waypoint.core.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;

public interface OrderRepository extends JpaRepository<Order, String> {

    boolean existsByRef(String ref);

    @Query("""
            select new com.synapse.waypoint.core.order.repository.OrderUnitTotals(
                sum(o.weightKg), sum(o.volumeM3), sum(o.units))
            from Order o
            where o.outletId = :outletId and o.temperatureRequirement = :temp and o.units > 0""")
    OrderUnitTotals totalsForOutlet(@Param("outletId") String outletId,
            @Param("temp") TemperatureRequirement temp);

    @Query("""
            select new com.synapse.waypoint.core.order.repository.OrderUnitTotals(
                sum(o.weightKg), sum(o.volumeM3), sum(o.units))
            from Order o
            where o.brand = :brand and o.temperatureRequirement = :temp and o.units > 0""")
    OrderUnitTotals totalsForBrand(@Param("brand") String brand, @Param("temp") TemperatureRequirement temp);
}
