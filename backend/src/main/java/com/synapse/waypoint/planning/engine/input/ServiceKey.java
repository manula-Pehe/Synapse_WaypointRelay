package com.synapse.waypoint.planning.engine.input;

import com.synapse.waypoint.planning.domain.Brand;
import com.synapse.waypoint.planning.domain.DockType;

/** Key of the service-allowance table. */
public record ServiceKey(Brand brand, DockType dockType) {
}
