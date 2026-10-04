package com.synapse.waypoint.planning.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.synapse.waypoint.planning.domain.StoreChoice;

/** What a store chose for a deferred order (table {@code deferral_choices}, V10). */
@Entity
@Table(name = "deferral_choices")
public class DeferralChoice {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "deferral_id", nullable = false, length = 40)
    private String deferralId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StoreChoice choice;

    /** Set for REDUCE and SPLIT. */
    private Integer units;

    @Column(name = "chosen_by", nullable = false, length = 40)
    private String chosenBy;

    @Column(name = "chosen_at", nullable = false)
    private Instant chosenAt;

    protected DeferralChoice() {
        // for JPA
    }

    public DeferralChoice(String id, String deferralId, StoreChoice choice, Integer units, String chosenBy,
            Instant chosenAt) {
        this.id = id;
        this.deferralId = deferralId;
        this.choice = choice;
        this.units = units;
        this.chosenBy = chosenBy;
        this.chosenAt = chosenAt;
    }

    public String getId() {
        return id;
    }

    public String getDeferralId() {
        return deferralId;
    }

    public StoreChoice getChoice() {
        return choice;
    }

    public Integer getUnits() {
        return units;
    }

    public String getChosenBy() {
        return chosenBy;
    }

    public Instant getChosenAt() {
        return chosenAt;
    }
}
