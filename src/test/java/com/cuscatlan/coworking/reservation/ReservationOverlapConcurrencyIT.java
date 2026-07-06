package com.cuscatlan.coworking.reservation;

import com.cuscatlan.coworking.reservation.dto.ReservationCreateRequest;
import com.cuscatlan.coworking.reservation.exception.OverlappingReservationException;
import com.cuscatlan.coworking.security.AuthenticatedUser;
import com.cuscatlan.coworking.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El test que justifica todo el diseño de concurrencia: dos hilos reservando el
 * mismo espacio en el mismo horario a la vez. Solo uno gana, y no por un if en el
 * service sino por la exclusion constraint disparandose bajo carrera real.
 */
class ReservationOverlapConcurrencyIT extends AbstractPostgresIT {

    // el admin sembrado por Flyway (V6) es el primer usuario -> id 1
    private static final AuthenticatedUser CALLER =
            new AuthenticatedUser(1L, "admin@coworking.sv", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    @Autowired
    private ReservationService service;

    @Test
    void dos_reservas_concurrentes_al_mismo_slot_solo_deja_pasar_una() throws Exception {
        // espacio 1 = "Sala Izalco" sembrado por Flyway
        ReservationCreateRequest req = new ReservationCreateRequest(1L,
                Instant.parse("2027-01-15T09:00:00Z"),
                Instant.parse("2027-01-15T11:00:00Z"));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> attempt = () -> {
            start.await();
            try {
                service.create(req, CALLER);
                return true;
            } catch (OverlappingReservationException e) {
                return false;
            }
        };

        Future<Boolean> first = pool.submit(attempt);
        Future<Boolean> second = pool.submit(attempt);
        start.countDown(); // largan al mismo tiempo

        List<Boolean> results = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        pool.shutdown();

        assertThat(results).containsExactlyInAnyOrder(true, false);
    }
}
