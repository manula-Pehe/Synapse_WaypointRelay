package com.synapse.waypoint.planning.engine;

import com.synapse.waypoint.planning.domain.Brand;
import com.synapse.waypoint.planning.engine.input.OrderInput;

/**
 * Ranks orders for the allocator: chilled Fresh 3, ambient Fresh 2, Style and Tech 1, plus 2 for every
 * day waited beyond the first and 3 when the order was deferred yesterday.
 */
public class PriorityScorer {

    static final int CHILLED_FRESH_BASE = 3;
    static final int AMBIENT_FRESH_BASE = 2;
    static final int STYLE_TECH_BASE = 1;
    static final int POINTS_PER_EXTRA_DAY_WAITED = 2;
    static final int DEFERRED_YESTERDAY_BONUS = 3;
    private static final int FIRST_DAY = 1;

    public int score(OrderInput order) {
        int extraDaysWaited = Math.max(0, order.daysSinceLastServed() - FIRST_DAY);
        int deferralBonus = order.deferredYesterday() ? DEFERRED_YESTERDAY_BONUS : 0;
        return baseScore(order) + POINTS_PER_EXTRA_DAY_WAITED * extraDaysWaited + deferralBonus;
    }

    private int baseScore(OrderInput order) {
        if (order.brand() != Brand.FRESH) {
            return STYLE_TECH_BASE;
        }
        return order.isChilled() ? CHILLED_FRESH_BASE : AMBIENT_FRESH_BASE;
    }
}
