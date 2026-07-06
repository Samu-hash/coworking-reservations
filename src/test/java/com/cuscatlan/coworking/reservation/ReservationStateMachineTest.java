package com.cuscatlan.coworking.reservation;

import com.cuscatlan.coworking.reservation.exception.IllegalReservationTransitionException;
import com.cuscatlan.coworking.space.Space;
import com.cuscatlan.coworking.space.SpaceType;
import com.cuscatlan.coworking.user.Role;
import com.cuscatlan.coworking.user.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationStateMachineTest {

    @Test
    void desde_pending_payment_se_puede_confirmar_o_cancelar() {
        assertThat(ReservationStatus.PENDING_PAYMENT.canTransitionTo(ReservationStatus.CONFIRMED)).isTrue();
        assertThat(ReservationStatus.PENDING_PAYMENT.canTransitionTo(ReservationStatus.CANCELLED)).isTrue();
        assertThat(ReservationStatus.PENDING_PAYMENT.canTransitionTo(ReservationStatus.COMPLETED)).isFalse();
    }

    @Test
    void cancelled_y_completed_son_terminales() {
        assertThat(ReservationStatus.CANCELLED.next()).isEmpty();
        assertThat(ReservationStatus.COMPLETED.next()).isEmpty();
    }

    @Test
    void no_se_puede_confirmar_una_reserva_ya_cancelada() {
        Reservation reservation = sample();
        reservation.transitionTo(ReservationStatus.CANCELLED);

        assertThatThrownBy(() -> reservation.transitionTo(ReservationStatus.CONFIRMED))
                .isInstanceOf(IllegalReservationTransitionException.class);
    }

    private Reservation sample() {
        Space space = new Space("Sala", SpaceType.MEETING_ROOM, 8, "Piso 3", new BigDecimal("10.00"));
        User user = new User("ana@test.sv", "hash", "Ana", Role.USER);
        return new Reservation(space, user,
                Instant.parse("2026-07-10T09:00:00Z"),
                Instant.parse("2026-07-10T10:00:00Z"),
                new BigDecimal("10.00"));
    }
}
