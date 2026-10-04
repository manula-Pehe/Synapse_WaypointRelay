package com.synapse.waypoint.core.file.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One uploaded image with its bytes (table {@code files}, V3). Written only through {@code StoredFileRepository}. */
@Entity
@Table(name = "files")
public class StoredFile {

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private FileKind kind;

    @Column(name = "content_type", nullable = false, length = 60)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Column(name = "data", nullable = false)
    private byte[] data;

    @Column(name = "client_id", length = 40)
    private String clientId;

    @Column(name = "uploaded_by", length = 40)
    private String uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StoredFile() {
        // for JPA
    }

    public String getId() {
        return id;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }
}
