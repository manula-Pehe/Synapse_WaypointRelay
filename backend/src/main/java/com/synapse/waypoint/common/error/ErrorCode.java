package com.synapse.waypoint.common.error;

import org.springframework.http.HttpStatus;

/**
 * Error codes shared by every module (see docs/api.md, "Error format").
 * Each code has exactly one HTTP status.
 */
public enum ErrorCode {

    VALIDATION(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    INVALID_STATUS(HttpStatus.CONFLICT),
    ORDERS_CLOSED(HttpStatus.CONFLICT),
    ORDERS_NOT_CLOSED(HttpStatus.CONFLICT),
    PLAN_LOCKED(HttpStatus.CONFLICT),
    RULE_VIOLATION(HttpStatus.CONFLICT),
    DUPLICATE(HttpStatus.CONFLICT),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
