package com.cuscatlan.coworking.space.dto;

import com.cuscatlan.coworking.space.SpaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record SpaceRequest(
        @NotBlank String name,
        @NotNull SpaceType type,
        @Positive int capacity,
        @NotBlank String location,
        @NotNull @PositiveOrZero BigDecimal hourlyRate
) {
}
