package com.cuscatlan.coworking.report.dto;

import java.time.LocalDate;
import java.util.List;

public record OccupancyReportResponse(LocalDate from, LocalDate to, List<SpaceOccupancy> spaces) {

    public record SpaceOccupancy(
            Long spaceId,
            String name,
            java.math.BigDecimal reservedHours,
            java.math.BigDecimal availableHours,
            java.math.BigDecimal occupancyPct
    ) {
    }
}
