package com.cuscatlan.coworking.payment;

import com.cuscatlan.coworking.reservation.ReservationService;
import com.cuscatlan.coworking.reservation.ReservationStatus;
import com.cuscatlan.coworking.reservation.dto.ReservationCreateRequest;
import com.cuscatlan.coworking.reservation.dto.ReservationResponse;
import com.cuscatlan.coworking.security.AuthenticatedUser;
import com.cuscatlan.coworking.support.AbstractPostgresIT;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * El gateway de pago caido (WireMock devolviendo 503) hace que el circuito abra y
 * que la reserva quede PENDING_PAYMENT via fallback, sin botarle un 500 al usuario.
 * Verifica los tres puntos del requisito de resiliencia: umbral, fallback coherente
 * y estado del circuito.
 */
class PaymentCircuitBreakerIT extends AbstractPostgresIT {

    // arranca antes de crear el contexto para poder registrar el puerto en las properties
    static final WireMockServer WIREMOCK = new WireMockServer(options().dynamicPort());

    static {
        WIREMOCK.start();
    }

    private static final AuthenticatedUser CALLER =
            new AuthenticatedUser(1L, "admin@coworking.sv", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void paymentProps(DynamicPropertyRegistry registry) {
        registry.add("app.payment-gateway.base-url", () -> "http://localhost:" + WIREMOCK.port());
    }

    @AfterAll
    static void stopWiremock() {
        WIREMOCK.stop();
    }

    @Test
    void cuando_el_gateway_falla_el_circuito_abre_y_la_reserva_queda_pendiente() {
        WIREMOCK.stubFor(post(urlEqualTo("/validate"))
                .willReturn(aResponse().withStatus(503)));

        // espacio 3 = cabina, distinto slot que otros tests para no chocar
        ReservationResponse created = reservationService.create(new ReservationCreateRequest(3L,
                Instant.parse("2027-02-20T09:00:00Z"),
                Instant.parse("2027-02-20T10:00:00Z")), CALLER);

        ReservationResponse last = null;
        for (int i = 0; i < 8; i++) {
            last = reservationService.confirm(created.id(), CALLER);
        }

        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("paymentGateway");
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(last).isNotNull();
        assertThat(last.status()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }
}
