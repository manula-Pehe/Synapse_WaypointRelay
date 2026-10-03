package com.synapse.waypoint.common.error;

import java.util.Map;

/**
 * Error body returned by every endpoint: {@code { code, message, details }}.
 */
public record ApiError(String code, String message, Map<String, Object> details) {

    public ApiError {
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static ApiError of(ErrorCode code, String message) {
        return new ApiError(code.name(), message, Map.of());
    }

    public static ApiError of(ErrorCode code, String message, Map<String, ?> details) {
        return new ApiError(code.name(), message, Map.copyOf(details));
    }
}
