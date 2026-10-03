package com.synapse.waypoint.core.settings.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Starting point of the demo: the demo clock's first "now" and the run date being planned.
 * Used only when nothing is stored yet.
 */
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(OffsetDateTime start, LocalDate runDate) {
}
