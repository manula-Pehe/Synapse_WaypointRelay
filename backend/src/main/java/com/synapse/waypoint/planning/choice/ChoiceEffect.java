package com.synapse.waypoint.planning.choice;

import com.synapse.waypoint.planning.domain.StoreChoice;

/** What one store choice does to the moved order. One implementation per {@link StoreChoice}. */
public interface ChoiceEffect {

    StoreChoice choice();

    /** Whether the units sent with the choice are kept on the stored choice. */
    default boolean takesUnits() {
        return false;
    }

    /** Throws a VALIDATION error when the choice cannot be applied; must not change anything. */
    default void validate(ChoiceContext context) {
    }

    void apply(ChoiceContext context);
}
