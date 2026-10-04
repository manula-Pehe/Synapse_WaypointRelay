package com.synapse.waypoint.core.file.service;

import com.synapse.waypoint.core.file.dto.FileContent;
import com.synapse.waypoint.core.file.entity.FileKind;

/** Stores and reads photos and signatures (driver proof, issue photos). docs/api.md §11. */
public interface FileService {

    /**
     * Saves an image and returns its id. When {@code clientId} (offline uploads, may be null) was
     * already stored, nothing is written and the existing id is returned, so retries are safe.
     *
     * @throws com.synapse.waypoint.common.error.DomainException {@code VALIDATION} for an empty file,
     *         a type other than jpeg, png or webp, or a file over 10 MB
     */
    String store(byte[] bytes, String contentType, FileKind kind, String clientId);

    /** @throws com.synapse.waypoint.common.error.NotFoundException when no file has this id */
    FileContent get(String fileId);
}
