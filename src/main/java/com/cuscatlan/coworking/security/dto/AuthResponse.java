package com.cuscatlan.coworking.security.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {
}
