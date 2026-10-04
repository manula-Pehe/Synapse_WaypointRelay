package com.synapse.waypoint.dispatch;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record LiveBoardDto(LocalDate runDate, String depot, OffsetDateTime generatedAt,
        Integer onTimePercent, int completedStops, int onTimeStops, Integer deferredToday,
        Integer skippedTwoRuns, Integer fridgeTruckUsePercent,
        List<Attention> needsAttention, List<TripProgress> trips) {

    public LiveBoardDto {
        needsAttention = List.copyOf(needsAttention);
        trips = List.copyOf(trips);
    }

    public record Attention(String id, String type, String severity, String title, String details,
            String orderId, String action) {}

    public record TripProgress(String id, String vehicleId, int tripNo, String district,
            int stopsDone, int stopsTotal, String status, OffsetDateTime lastUpdate,
            OffsetDateTime lastSync) {}
}
