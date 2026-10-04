package com.synapse.waypoint.planning.choice;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.planning.domain.StoreChoice;

/** Finds the effect of a choice; startup fails if a {@link StoreChoice} has none. */
@Component
public class ChoiceEffects {

    private final Map<StoreChoice, ChoiceEffect> byChoice = new EnumMap<>(StoreChoice.class);

    ChoiceEffects(List<ChoiceEffect> effects) {
        effects.forEach(effect -> byChoice.put(effect.choice(), effect));
        for (StoreChoice choice : StoreChoice.values()) {
            if (!byChoice.containsKey(choice)) {
                throw new IllegalStateException("No effect for store choice " + choice);
            }
        }
    }

    public ChoiceEffect of(StoreChoice choice) {
        return byChoice.get(choice);
    }
}
