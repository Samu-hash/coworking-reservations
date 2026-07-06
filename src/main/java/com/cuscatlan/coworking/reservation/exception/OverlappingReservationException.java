package com.cuscatlan.coworking.reservation.exception;

import java.time.Instant;

public class OverlappingReservationException extends RuntimeException {

    public OverlappingReservationException(Long spaceId, Instant start, Instant end) {
        super("El espacio " + spaceId + " ya tiene una reserva en ese horario (" + start + " a " + end + ")");
    }
}
