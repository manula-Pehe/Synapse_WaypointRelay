package com.synapse.waypoint.planning.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;

import com.synapse.waypoint.planning.engine.input.DeliveryWindow;

/**
 * Arrival window = predicted arrival ± 15 minutes, clipped to the outlet's window.
 * Late risk = 1 − slack ÷ 60, limited to 0–1, where slack is the minutes between the predicted arrival
 * and the window closing: an hour or more of slack is risk 0, arriving at or after close is risk 1.
 * A late arrival shows the window collapsed to the closing time.
 */
public class ArrivalEstimator {

    static final int MARGIN_MINUTES = 15;
    static final int RISK_HORIZON_MINUTES = 60;
    private static final int RISK_SCALE = 3;

    public ArrivalWindow estimate(DeliveryWindow window, LocalTime predictedArrival) {
        LocalTime to = earlier(predictedArrival.plusMinutes(MARGIN_MINUTES), window.close());
        LocalTime from = earlier(later(predictedArrival.minusMinutes(MARGIN_MINUTES), window.open()), to);
        return new ArrivalWindow(from, to, lateRisk(window, predictedArrival));
    }

    private BigDecimal lateRisk(DeliveryWindow window, LocalTime predictedArrival) {
        long slackMinutes = Duration.between(predictedArrival, window.close()).toMinutes();
        BigDecimal risk = BigDecimal.ONE.subtract(
                BigDecimal.valueOf(slackMinutes).divide(BigDecimal.valueOf(RISK_HORIZON_MINUTES), RISK_SCALE, RoundingMode.HALF_UP));
        return risk.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private static LocalTime later(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalTime earlier(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }
}
