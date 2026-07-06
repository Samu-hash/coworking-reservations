package com.cuscatlan.coworking.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ReservationCreateRequest(
        @NotNull Long spaceId,
        @NotNull @Future Instant startTime,
        @NotNull Instant endTime
) {
}
