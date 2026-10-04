package com.synapse.waypoint.dispatch;

import java.time.LocalDate;
import java.util.List;

public record RunReportDto(LocalDate runDate, String depot, Integer onTimePercent, int onTimeStops,
        int completedStops, Integer deferred, int failed, int partial,
        List<DistrictLate> lateByDistrict, List<ExceptionItem> exceptions) {

    public RunReportDto {
        lateByDistrict = List.copyOf(lateByDistrict);
        exceptions = List.copyOf(exceptions);
    }

    public record DistrictLate(String district, int late, int completed) {}

    public record ExceptionItem(String type, String detail, String orderId) {}
}
