package com.synapse.waypoint.core.seed;

/** The dataset folder, a file in it, or a value in a file is missing or unusable. */
public class DatasetException extends RuntimeException {

    public DatasetException(String message) {
        super(message);
    }

    public DatasetException(String message, Throwable cause) {
        super(message, cause);
    }
}
