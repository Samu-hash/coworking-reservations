package com.cuscatlan.coworking.space.dto;

import com.cuscatlan.coworking.space.SpaceType;

import java.math.BigDecimal;

public record SpaceResponse(
        Long id,
        String name,
        SpaceType type,
        int capacity,
        String location,
        BigDecimal hourlyRate,
        boolean active
) {
}
