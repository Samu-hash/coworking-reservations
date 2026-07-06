package com.cuscatlan.coworking.security;

import com.cuscatlan.coworking.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seguridad de punta a punta con tokens de verdad (no @WithMockUser, que se saltaria
 * el filtro JWT). Cubre: sin token 401, endpoint de admin con USER 403, y ownership
 * (un USER que pide una reserva ajena recibe 404, no 403, para no filtrar existencia).
 */
class AuthorizationAndOwnershipIT extends AbstractPostgresIT {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void sin_token_devuelve_401() {
        ResponseEntity<String> res = rest.getForEntity("/spaces", String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void un_user_no_puede_pegarle_a_un_endpoint_de_admin() {
        register("user1@test.sv", "secret1", "User Uno");
        String token = login("user1@test.sv", "secret1");

        var body = Map.of("name", "Sala X", "type", "MEETING_ROOM", "capacity", 4,
                "location", "P1", "hourlyRate", 10.0);
        ResponseEntity<String> res = rest.exchange("/spaces", HttpMethod.POST,
                new HttpEntity<>(body, bearer(token)), String.class);

        assertThat(res.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @SuppressWarnings("unchecked")
    void un_user_no_ve_la_reserva_de_otro_pero_el_admin_si() {
        register("dueno@test.sv", "secret1", "Dueno");
        register("ajeno@test.sv", "secret1", "Ajeno");
        String ownerToken = login("dueno@test.sv", "secret1");
        String otherToken = login("ajeno@test.sv", "secret1");
        String adminToken = login("admin@coworking.sv", "admin1234");

        // el dueno crea una reserva (espacio 2 sembrado por Flyway)
        var reservation = Map.of("spaceId", 2, "startTime", "2027-03-10T09:00:00Z", "endTime", "2027-03-10T10:00:00Z");
        ResponseEntity<Map> created = rest.exchange("/reservations", HttpMethod.POST,
                new HttpEntity<>(reservation, bearer(ownerToken)), Map.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        Integer id = (Integer) created.getBody().get("id");

        // un usuario ajeno: 404 (no 403)
        ResponseEntity<String> ajeno = rest.exchange("/reservations/" + id, HttpMethod.GET,
                new HttpEntity<>(bearer(otherToken)), String.class);
        assertThat(ajeno.getStatusCode().value()).isEqualTo(404);

        // el admin la ve
        ResponseEntity<String> admin = rest.exchange("/reservations/" + id, HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), String.class);
        assertThat(admin.getStatusCode().value()).isEqualTo(200);
    }

    private void register(String email, String password, String name) {
        var body = Map.of("email", email, "password", password, "fullName", name);
        ResponseEntity<String> res = rest.exchange("/auth/register", HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders()), String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String login(String email, String password) {
        var body = Map.of("email", email, "password", password);
        ResponseEntity<Map> res = rest.exchange("/auth/login", HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        return (String) res.getBody().get("accessToken");
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
