package com.cuscatlan.coworking.reservation.event;

public record ReservationConfirmedEvent(Long reservationId, Long spaceId, String userEmail) {
}
