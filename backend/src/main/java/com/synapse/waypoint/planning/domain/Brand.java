package com.synapse.waypoint.planning.domain;

import java.util.Arrays;

/** The three Waypoint brands; {@code label} is how the dataset and API write them. */
public enum Brand {
    FRESH("Fresh", TripWindowType.FRESH),
    STYLE("Style", TripWindowType.DAYTIME),
    TECH("Tech", TripWindowType.DAYTIME);

    private final String label;
    private final TripWindowType windowType;

    Brand(String label, TripWindowType windowType) {
        this.label = label;
        this.windowType = windowType;
    }

    public String label() {
        return label;
    }

    /** Fresh trips run pre-dawn; Style and Tech trips run in the daytime. */
    public TripWindowType windowType() {
        return windowType;
    }

    public static Brand fromLabel(String label) {
        return Arrays.stream(values())
                .filter(brand -> brand.label.equalsIgnoreCase(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown brand: " + label));
    }
}
