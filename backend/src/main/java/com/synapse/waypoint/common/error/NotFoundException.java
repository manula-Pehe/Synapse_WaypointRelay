package com.synapse.waypoint.common.error;

import java.util.Map;

/**
 * A resource does not exist, or belongs to another outlet, depot or vehicle
 * (we never reveal which).
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String resource, String id) {
        super(ErrorCode.NOT_FOUND, resource + " not found", Map.of("resource", resource, "id", id));
    }
}
