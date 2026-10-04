package com.synapse.waypoint.common.error;

import java.util.Map;

/**
 * Base class for business errors. Throw a subclass (or this class with a specific code)
 * from services; {@link GlobalExceptionHandler} turns it into the standard error body.
 */
public class DomainException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> details;

    public DomainException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public DomainException(ErrorCode code, String message, Map<String, ?> details) {
        super(message);
        this.code = code;
        this.details = Map.copyOf(details);
    }

    public ErrorCode code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
