package com.cuscatlan.coworking.security;

import com.cuscatlan.coworking.config.properties.JwtProperties;
import com.cuscatlan.coworking.user.Role;
import com.cuscatlan.coworking.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new JwtProperties("clave-de-prueba-con-mas-de-treinta-y-dos-chars", Duration.ofHours(1), "coworking-api"));

    @Test
    void genera_un_token_que_puede_volver_a_parsear() {
        User user = new User("ana@test.sv", "hash", "Ana Prueba", Role.USER);

        AuthenticatedUser parsed = jwtService.parse(jwtService.generate(user));

        assertThat(parsed.username()).isEqualTo("ana@test.sv");
        assertThat(parsed.authorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        assertThat(parsed.isAdmin()).isFalse();
    }
}
