package com.synapse.waypoint.notification.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.waypoint.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByUserIdOrderByCreatedAtDescIdDesc(String userId);

    List<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(String userId);

    long countByUserIdAndReadAtIsNull(String userId);

    Optional<Notification> findByIdAndUserId(String id, String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.readAt = :at where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") String userId, @Param("at") Instant at);
}
