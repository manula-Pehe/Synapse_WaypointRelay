package com.synapse.waypoint.loader;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Loader facts for the store, dispatch and driver modules. */
public interface LoadingQueryService {
    List<LoaderService.TripSummary> loadingStatus(LocalDate runDate, String depot);
    Optional<LoaderService.ShortfallResult> shortfallForOrder(String orderId);
}
