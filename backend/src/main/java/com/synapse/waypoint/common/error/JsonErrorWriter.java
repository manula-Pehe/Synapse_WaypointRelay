package com.synapse.waypoint.common.error;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

/**
 * Writes a fixed {@link ApiError} body from servlet filters (security), where
 * {@link GlobalExceptionHandler} is not reached. Messages are constants, so no escaping is needed.
 */
public final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    public static void write(HttpServletResponse response, ErrorCode code, String message) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"code\":\"" + code.name() + "\",\"message\":\"" + message + "\",\"details\":{}}");
    }
}
