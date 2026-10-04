package com.synapse.waypoint.core.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.repository.OrderUnitTotals;
import com.synapse.waypoint.core.order.support.OrderFixtures;

class OrderSizeEstimatorTests {

    private static final String OUTLET = "OUT991";
    private static final String BRAND = "Fresh";
    private static final TemperatureRequirement TEMP = TemperatureRequirement.CHILLED;
    private static final OrderUnitTotals NO_ORDERS = new OrderUnitTotals(null, null, null);

    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderSizeEstimator estimator = new OrderSizeEstimator(orders);

    @Test
    void shouldUseTheOutletsOwnPerUnitAverageFirst() {
        when(orders.totalsForOutlet(OUTLET, TEMP)).thenReturn(totals("200.00", "2.000", 40));
        when(orders.totalsForBrand(BRAND, TEMP)).thenReturn(totals("999.00", "9.000", 10));

        OrderSize size = estimator.estimate(OUTLET, BRAND, TEMP, 10);

        assertThat(size.weightKg()).isEqualByComparingTo("50.00");
        assertThat(size.volumeM3()).isEqualByComparingTo("0.500");
    }

    @Test
    void shouldFallBackToTheBrandAndTemperatureAverageWhenTheOutletHasNoOrders() {
        when(orders.totalsForOutlet(OUTLET, TEMP)).thenReturn(NO_ORDERS);
        when(orders.totalsForBrand(BRAND, TEMP)).thenReturn(totals("300.00", "3.000", 100));

        OrderSize size = estimator.estimate(OUTLET, BRAND, TEMP, 10);

        assertThat(size.weightKg()).isEqualByComparingTo("30.00");
        assertThat(size.volumeM3()).isEqualByComparingTo("0.300");
    }

    @Test
    void shouldFallBackToTheNamedDefaultsWhenNothingIsKnown() {
        when(orders.totalsForOutlet(OUTLET, TEMP)).thenReturn(NO_ORDERS);
        when(orders.totalsForBrand(BRAND, TEMP)).thenReturn(NO_ORDERS);

        OrderSize size = estimator.estimate(OUTLET, BRAND, TEMP, 10);

        assertThat(size.weightKg()).isEqualByComparingTo(
                OrderSizeEstimator.DEFAULT_WEIGHT_KG_PER_UNIT.multiply(BigDecimal.TEN));
        assertThat(size.volumeM3()).isEqualByComparingTo(
                OrderSizeEstimator.DEFAULT_VOLUME_M3_PER_UNIT.multiply(BigDecimal.TEN));
    }

    @Test
    void shouldScaleAnOrdersOwnSizeInProportionForARemainder() {
        Order parent = Order.create(OrderFixtures.newOrder(OUTLET, OrderStatus.CONFIRMED, TEMP, 10,
                OrderSource.STORE, OrderFixtures.RUN_DATE), OrderFixtures.CREATED_AT);

        OrderSize size = estimator.proportionalTo(parent, 3);

        assertThat(size.weightKg()).isEqualByComparingTo("30.00");
        assertThat(size.volumeM3()).isEqualByComparingTo("0.300");
    }

    private static OrderUnitTotals totals(String weightKg, String volumeM3, long units) {
        return new OrderUnitTotals(new BigDecimal(weightKg), new BigDecimal(volumeM3), units);
    }
}
