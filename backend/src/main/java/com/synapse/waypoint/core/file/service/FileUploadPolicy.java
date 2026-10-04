package com.synapse.waypoint.core.file.service;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;

/** Which uploads are accepted: image types and a size limit (matches the nginx body limit). */
@Component
class FileUploadPolicy {

    static final int MAX_SIZE_BYTES = 10 * 1024 * 1024;
    static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    void check(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw invalid("The file is empty.", Map.of());
        }
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw invalid("Only JPEG, PNG or WebP images are accepted.", Map.of("allowed", ALLOWED_TYPES));
        }
        if (bytes.length > MAX_SIZE_BYTES) {
            throw invalid("The file is too large. The limit is 10 MB.", Map.of("maxBytes", MAX_SIZE_BYTES));
        }
    }

    private static DomainException invalid(String message, Map<String, ?> details) {
        return new DomainException(ErrorCode.VALIDATION, message, details);
    }
}
