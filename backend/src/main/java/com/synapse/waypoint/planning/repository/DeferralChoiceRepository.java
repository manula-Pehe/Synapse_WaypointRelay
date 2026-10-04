package com.synapse.waypoint.planning.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.waypoint.planning.entity.DeferralChoice;

public interface DeferralChoiceRepository extends JpaRepository<DeferralChoice, String> {

    List<DeferralChoice> findByDeferralId(String deferralId);

    List<DeferralChoice> findByDeferralIdIn(Collection<String> deferralIds);

    boolean existsByDeferralId(String deferralId);

    /**
     * Stores the choice unless the deferral already has one, and says how many rows it stored. A conflict
     * stores nothing and raises no database error, so the caller's transaction stays usable.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO deferral_choices (id, deferral_id, choice, units, chosen_by, chosen_at)
            VALUES (:id, :deferralId, :choice, :units, :chosenBy, :chosenAt)
            ON CONFLICT (deferral_id) DO NOTHING""", nativeQuery = true)
    int insertIfAbsent(@Param("id") String id, @Param("deferralId") String deferralId,
            @Param("choice") String choice, @Param("units") Integer units, @Param("chosenBy") String chosenBy,
            @Param("chosenAt") Instant chosenAt);
}
