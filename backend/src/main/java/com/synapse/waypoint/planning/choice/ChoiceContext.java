package com.synapse.waypoint.planning.choice;

import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.planning.entity.Deferral;

/** What a choice effect needs to know: the deferral, the moved order as it is now, and the units sent. */
public record ChoiceContext(Deferral deferral, OrderDto order, Integer units) {
}
