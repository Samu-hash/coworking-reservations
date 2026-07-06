package com.cuscatlan.coworking.report;

import com.cuscatlan.coworking.report.dto.OccupancyReportResponse;
import com.cuscatlan.coworking.reservation.Reservation;
import com.cuscatlan.coworking.reservation.ReservationRepository;
import com.cuscatlan.coworking.reservation.ReservationStatus;
import com.cuscatlan.coworking.space.Space;
import com.cuscatlan.coworking.space.SpaceService;
import com.cuscatlan.coworking.support.AbstractPostgresIT;
import com.cuscatlan.coworking.user.User;
import com.cuscatlan.coworking.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El reporte se apoya en una query nativa con casts y tstzrange; solo se puede
 * probar de verdad contra Postgres. Cubre el calculo de ocupacion de una reserva
 * confirmada (4h en un dia de 24h -> ~16.67%).
 */
class OccupancyReportIT extends AbstractPostgresIT {

    @Autowired
    private OccupancyReportService reportService;
    @Autowired
    private ReservationRepository reservations;
    @Autowired
    private SpaceService spaceService;
    @Autowired
    private UserRepository users;

    @Test
    void calcula_la_ocupacion_de_una_reserva_confirmada() {
        Space space = spaceService.getEntity(1L);
        User admin = users.findById(1L).orElseThrow();

        Reservation reservation = new Reservation(space, admin,
                Instant.parse("2027-09-10T09:00:00Z"),
                Instant.parse("2027-09-10T13:00:00Z"),
                new BigDecimal("50.00"));
        reservation.transitionTo(ReservationStatus.CONFIRMED);
        reservations.saveAndFlush(reservation);

        OccupancyReportResponse report = reportService.occupancy(
                LocalDate.parse("2027-09-10"), LocalDate.parse("2027-09-10"));

        var sala = report.spaces().stream()
                .filter(s -> s.spaceId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(sala.reservedHours()).isEqualByComparingTo("4.00");
        assertThat(sala.occupancyPct()).isEqualByComparingTo("16.67");
    }
}
