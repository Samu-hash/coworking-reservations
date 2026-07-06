package com.cuscatlan.coworking.payment;

import java.math.BigDecimal;

public record PaymentRequest(Long reservationId, BigDecimal amount) {
}
