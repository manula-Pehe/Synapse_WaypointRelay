package com.synapse.waypoint.driver.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.synapse.waypoint.core.file.entity.FileKind;
import com.synapse.waypoint.core.file.service.FileService;

/**
 * Proof uploads from the driver's phone - docs/api.md §7, F5 (R4p photo, R4s signature).
 *
 * The phone sends {@code clientId} with every file, so a queued upload retried after a dropped
 * connection is stored once rather than twice (US-1.2). The returned id is what goes into the
 * delivery; the bytes are read back from {@code GET /api/files/{id}}.
 */
@RestController
@RequestMapping("/api/driver/files")
class DriverFileController {

    private final FileService files;

    DriverFileController(FileService files) {
        this.files = files;
    }

    /**
     * Stores one image and returns its id. {@code kind} is PHOTO or SIGNATURE; anything else is a
     * validation error, as is a file that is empty or over the size policy.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileUploadResponse upload(@RequestParam("file") MultipartFile file,
            @RequestParam("kind") String kind,
            @RequestParam("clientId") String clientId) throws java.io.IOException {
        return new FileUploadResponse(
                files.store(file.getBytes(), file.getContentType(), kindOf(kind), clientId));
    }

    private FileKind kindOf(String kind) {
        return switch (kind == null ? "" : kind.toUpperCase()) {
            case "PHOTO" -> FileKind.PHOTO;
            case "SIGNATURE" -> FileKind.SIGNATURE;
            default -> throw new com.synapse.waypoint.common.error.DomainException(
                    com.synapse.waypoint.common.error.ErrorCode.VALIDATION,
                    "kind must be PHOTO or SIGNATURE");
        };
    }

    /** The stored id; the phone puts it on the queued delivery action. */
    record FileUploadResponse(String id) {
    }
}