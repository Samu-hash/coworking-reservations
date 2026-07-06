package com.cuscatlan.coworking.reservation.exception;

import com.cuscatlan.coworking.reservation.ReservationStatus;

public class IllegalReservationTransitionException extends RuntimeException {

    public IllegalReservationTransitionException(ReservationStatus from, ReservationStatus to) {
        super("No se puede pasar la reserva de " + from + " a " + to);
    }
}
