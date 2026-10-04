package com.synapse.waypoint.planning.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.planning.entity.DeferralChoice;

public interface DeferralChoiceRepository extends JpaRepository<DeferralChoice, String> {

    List<DeferralChoice> findByDeferralId(String deferralId);

    List<DeferralChoice> findByDeferralIdIn(Collection<String> deferralIds);
}
