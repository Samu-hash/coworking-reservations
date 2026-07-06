package com.cuscatlan.coworking.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, message = "la contrasena debe tener al menos 6 caracteres") String password,
        @NotBlank String fullName
) {
}
