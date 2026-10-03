package com.synapse.waypoint.core.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.repository.OrderUnitTotals;

/**
 * Estimates the weight and volume of an order that only states its units (store extra orders,
 * phone-in orders). It uses the per-unit average of the same outlet and temperature, else of the
 * same brand and temperature across all outlets, else fixed defaults.
 */
@Component
class OrderSizeEstimator {

    static final BigDecimal DEFAULT_WEIGHT_KG_PER_UNIT = new BigDecimal("5.00");
    static final BigDecimal DEFAULT_VOLUME_M3_PER_UNIT = new BigDecimal("0.030");

    private static final int WEIGHT_SCALE = 2;
    private static final int VOLUME_SCALE = 3;
    private static final OrderUnitTotals DEFAULT_PER_UNIT = new OrderUnitTotals(
            DEFAULT_WEIGHT_KG_PER_UNIT, DEFAULT_VOLUME_M3_PER_UNIT, 1L);

    private final OrderRepository orders;

    OrderSizeEstimator(OrderRepository orders) {
        this.orders = orders;
    }

    OrderSize estimate(String outletId, String brand, TemperatureRequirement temp, int units) {
        OrderUnitTotals basis = withHistory(orders.totalsForOutlet(outletId, temp))
                .or(() -> withHistory(orders.totalsForBrand(brand, temp)))
                .orElse(DEFAULT_PER_UNIT);
        return scaled(basis, units);
    }

    /** The size of {@code units} of the same goods as {@code basis}, in proportion to its own size. */
    OrderSize proportionalTo(Order basis, int units) {
        if (basis.getUnits() <= 0) {
            return estimate(basis.getOutletId(), basis.getBrand(), basis.getTemperatureRequirement(), units);
        }
        return scaled(new OrderUnitTotals(basis.getWeightKg(), basis.getVolumeM3(), (long) basis.getUnits()), units);
    }

    private static Optional<OrderUnitTotals> withHistory(OrderUnitTotals totals) {
        return Optional.ofNullable(totals).filter(OrderUnitTotals::hasUnits);
    }

    private static OrderSize scaled(OrderUnitTotals basis, int units) {
        BigDecimal basisUnits = BigDecimal.valueOf(basis.units());
        BigDecimal requested = BigDecimal.valueOf(units);
        return new OrderSize(
                basis.weightKg().multiply(requested).divide(basisUnits, WEIGHT_SCALE, RoundingMode.HALF_UP),
                basis.volumeM3().multiply(requested).divide(basisUnits, VOLUME_SCALE, RoundingMode.HALF_UP));
    }
}
