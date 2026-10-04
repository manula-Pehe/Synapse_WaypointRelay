package com.synapse.waypoint.planning.engine;

import static com.synapse.waypoint.planning.engine.PlanningTestData.anOrder;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.planning.domain.Brand;

class PriorityScorerTests {

    private final PriorityScorer scorer = new PriorityScorer();

    @Test
    void shouldScoreChilledFreshThree() {
        assertThat(scorer.score(anOrder().chilled().build())).isEqualTo(3);
    }

    @Test
    void shouldScoreAmbientFreshTwo() {
        assertThat(scorer.score(anOrder().build())).isEqualTo(2);
    }

    @Test
    void shouldScoreStyleAndTechOne() {
        assertThat(scorer.score(anOrder().brand(Brand.STYLE).build())).isEqualTo(1);
        assertThat(scorer.score(anOrder().brand(Brand.TECH).build())).isEqualTo(1);
    }

    @Test
    void shouldAddTwoPerDayWaitedBeyondTheFirst() {
        assertThat(scorer.score(anOrder().daysSinceLastServed(1).build())).isEqualTo(2);
        assertThat(scorer.score(anOrder().daysSinceLastServed(4).build())).isEqualTo(2 + 6);
    }

    @Test
    void shouldAddThreeWhenDeferredYesterday() {
        assertThat(scorer.score(anOrder().chilled().deferredYesterday().build())).isEqualTo(3 + 3);
    }

    @Test
    void shouldCombineWaitingAndDeferralBonuses() {
        assertThat(scorer.score(anOrder().brand(Brand.TECH).daysSinceLastServed(3).deferredYesterday().build()))
                .isEqualTo(1 + 4 + 3);
    }
}
