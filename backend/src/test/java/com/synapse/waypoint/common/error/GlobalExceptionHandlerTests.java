package com.synapse.waypoint.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldUseTheCodesStatusAndMessageForDomainErrors() {
        ResponseEntity<ApiError> response = handler.handleDomain(
                new DomainException(ErrorCode.ORDERS_CLOSED, "Orders closed", Map.of("runDate", "2026-10-01")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(
                new ApiError("ORDERS_CLOSED", "Orders closed", Map.of("runDate", "2026-10-01")));
    }

    @Test
    void shouldReturn404WithResourceDetailsWhenNotFound() {
        ResponseEntity<ApiError> response = handler.handleDomain(new NotFoundException("Order", "ord-1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().details()).containsEntry("id", "ord-1");
    }

    @Test
    void shouldHideInternalDetailsForUnexpectedErrors() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(new IllegalStateException("db password is x"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("password");
    }

    @Test
    void shouldReturnConflictAskingToReloadWhenSomeoneElseChangedTheRecord() {
        ResponseEntity<ApiError> response = handler.handleConcurrentChange(
                new OptimisticLockingFailureException("stale row"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(new ApiError("CONFLICT",
                "This was changed by someone else. Please reload.", Map.of()));
    }
}
