package com.synapse.waypoint.core.file.dto;

/** A stored file as callers see it: its id, media type and bytes. */
public record FileContent(String id, String contentType, byte[] bytes) {
}
