package com.synapse.waypoint.planning.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.planning.choice.ChoiceContext;
import com.synapse.waypoint.planning.choice.ChoiceEffect;
import com.synapse.waypoint.planning.choice.ChoiceEffects;
import com.synapse.waypoint.planning.domain.StoreChoice;
import com.synapse.waypoint.planning.dto.DeferralDto;
import com.synapse.waypoint.planning.entity.Deferral;
import com.synapse.waypoint.planning.repository.DeferralChoiceRepository;
import com.synapse.waypoint.planning.repository.DeferralRepository;

/**
 * Checks first, changes the order next and stores the choice last. The store's insert ignores a
 * conflict instead of failing, so two simultaneous answers end as DUPLICATE for the second one
 * without a database error; its exception then undoes that request's order change.
 */
@Service
@Transactional
class DefaultDeferralChoiceService implements DeferralChoiceService {

    private static final String ID_PREFIX = "dch-";

    private final DeferralRepository deferrals;
    private final DeferralChoiceRepository choices;
    private final OrderService orders;
    private final ChoiceEffects effects;
    private final DeferralChoiceNotifier notifier;
    private final PlanViewLoader views;
    private final CurrentUser currentUser;
    private final DemoClock clock;

    DefaultDeferralChoiceService(DeferralRepository deferrals, DeferralChoiceRepository choices, OrderService orders,
            ChoiceEffects effects, DeferralChoiceNotifier notifier, PlanViewLoader views, CurrentUser currentUser,
            DemoClock clock) {
        this.deferrals = deferrals;
        this.choices = choices;
        this.orders = orders;
        this.effects = effects;
        this.notifier = notifier;
        this.views = views;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    public DeferralDto choose(String deferralId, StoreChoice choice, Integer units) {
        Deferral deferral = deferrals.findPublishedById(deferralId)
                .orElseThrow(() -> new NotFoundException("Deferral", deferralId));
        OrderDto order = orders.get(deferral.getOrderId());
        requireNoChoiceYet(deferralId);
        requireMoved(order);
        ChoiceContext context = new ChoiceContext(deferral, order, units);
        ChoiceEffect effect = effects.of(choice);
        effect.validate(context);
        effect.apply(context);
        store(deferralId, choice, effect.takesUnits() ? units : null);
        notifier.notifyDispatchers(order, choice, units);
        return views.toDtos(List.of(deferral)).get(0);
    }

    private void requireNoChoiceYet(String deferralId) {
        if (choices.existsByDeferralId(deferralId)) {
            throw alreadyChosen();
        }
    }

    private static void requireMoved(OrderDto order) {
        if (order.status() != OrderStatus.MOVED) {
            throw new DomainException(ErrorCode.INVALID_STATUS,
                    "Only a moved order can be answered, this one is " + order.status() + ".");
        }
    }

    private void store(String deferralId, StoreChoice choice, Integer units) {
        int stored = choices.insertIfAbsent(ID_PREFIX + UUID.randomUUID(), deferralId, choice.name(), units,
                currentUser.id(), clock.now());
        if (stored == 0) {
            throw alreadyChosen();
        }
    }

    private static DomainException alreadyChosen() {
        return new DomainException(ErrorCode.DUPLICATE, "This deferral has been answered already.");
    }
}
