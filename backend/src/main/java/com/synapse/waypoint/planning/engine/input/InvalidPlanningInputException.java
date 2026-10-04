package com.synapse.waypoint.planning.engine.input;

/** The planning input lacks data the engine needs, such as travel times for a district. */
public class InvalidPlanningInputException extends RuntimeException {

    public InvalidPlanningInputException(String message) {
        super(message);
    }
}
