package com.cuscatlan.coworking.payment;

public record PaymentValidationResult(String status, String reference) {

    public boolean approved() {
        return "APPROVED".equalsIgnoreCase(status);
    }

    // usado por el fallback del circuit breaker: ni aprueba ni rechaza, deja la
    // reserva pendiente de pago
    public static PaymentValidationResult pending() {
        return new PaymentValidationResult("PENDING", null);
    }
}
