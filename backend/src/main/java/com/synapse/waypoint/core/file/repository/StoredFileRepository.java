package com.synapse.waypoint.core.file.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.waypoint.core.file.entity.StoredFile;

public interface StoredFileRepository extends JpaRepository<StoredFile, String> {

    /**
     * Inserts the file unless one with the same {@code clientId} exists. Done in SQL so a retry
     * racing the first upload never raises a constraint error inside the caller's transaction.
     *
     * @return 1 when a row was written, 0 when the {@code clientId} was already taken
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO files (id, kind, content_type, size_bytes, data, client_id, uploaded_by, created_at)
            VALUES (:id, :kind, :contentType, :sizeBytes, :data, :clientId, :uploadedBy, :createdAt)
            ON CONFLICT (client_id) DO NOTHING
            """)
    int insertIfClientIdFree(@Param("id") String id, @Param("kind") String kind,
            @Param("contentType") String contentType, @Param("sizeBytes") int sizeBytes,
            @Param("data") byte[] data, @Param("clientId") String clientId,
            @Param("uploadedBy") String uploadedBy, @Param("createdAt") Instant createdAt);

    @Query("select f.id from StoredFile f where f.clientId = :clientId")
    Optional<String> findIdByClientId(@Param("clientId") String clientId);
}
