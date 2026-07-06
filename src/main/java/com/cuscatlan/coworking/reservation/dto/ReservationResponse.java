package com.cuscatlan.coworking.reservation.dto;

import com.cuscatlan.coworking.reservation.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ReservationResponse(
        Long id,
        SpaceSummary space,
        UserSummary user,
        Instant startTime,
        Instant endTime,
        ReservationStatus status,
        BigDecimal priceTotal
) {

    public record SpaceSummary(Long id, String name) {
    }

    public record UserSummary(Long id, String fullName) {
    }
}
