package com.cuscatlan.coworking.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// el SecurityScheme deja pegar el token en el boton "Authorize" de Swagger UI
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Coworking Reservations API", version = "0.1.0",
                description = "Gestion de reservas de espacios de coworking"),
        security = @SecurityRequirement(name = "bearer-jwt"))
@SecurityScheme(name = "bearer-jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class OpenApiConfig {
}
