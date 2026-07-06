package com.cuscatlan.coworking.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(

        // HS256 exige clave de al menos 256 bits; si es mas corta jjwt tira WeakKeyException al firmar
        @NotBlank @Size(min = 32, message = "el secreto jwt necesita al menos 32 caracteres") String secret,

        @NotNull Duration accessTtl,

        @NotBlank String issuer
) {
}
