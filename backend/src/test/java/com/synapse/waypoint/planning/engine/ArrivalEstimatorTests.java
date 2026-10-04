package com.synapse.waypoint.planning.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.planning.engine.input.DeliveryWindow;

class ArrivalEstimatorTests {

    private final ArrivalEstimator estimator = new ArrivalEstimator();
    private final DeliveryWindow window = new DeliveryWindow(LocalTime.of(4, 0), LocalTime.of(8, 0));

    @Test
    void shouldPredictFifteenMinutesEitherSide() {
        ArrivalWindow arrival = estimator.estimate(window, LocalTime.of(5, 30));

        assertThat(arrival.from()).isEqualTo(LocalTime.of(5, 15));
        assertThat(arrival.to()).isEqualTo(LocalTime.of(5, 45));
    }

    @Test
    void shouldClipTheWindowToTheOutletOpeningTime() {
        assertThat(estimator.estimate(window, LocalTime.of(4, 5)).from()).isEqualTo(LocalTime.of(4, 0));
    }

    @Test
    void shouldClipTheWindowToTheOutletClosingTime() {
        assertThat(estimator.estimate(window, LocalTime.of(7, 50)).to()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void shouldHaveNoLateRiskWithAnHourOrMoreOfSlack() {
        assertThat(estimator.estimate(window, LocalTime.of(7, 0)).lateRisk()).isEqualByComparingTo("0");
        assertThat(estimator.estimate(window, LocalTime.of(5, 0)).lateRisk()).isEqualByComparingTo("0");
    }

    @Test
    void shouldHaveFullLateRiskWhenArrivingExactlyAtClose() {
        assertThat(estimator.estimate(window, LocalTime.of(8, 0)).lateRisk()).isEqualByComparingTo("1");
    }

    @Test
    void shouldScaleLateRiskWithTheSlackLeft() {
        assertThat(estimator.estimate(window, LocalTime.of(7, 30)).lateRisk()).isEqualByComparingTo("0.5");
        assertThat(estimator.estimate(window, LocalTime.of(7, 45)).lateRisk()).isEqualByComparingTo("0.75");
    }
}
