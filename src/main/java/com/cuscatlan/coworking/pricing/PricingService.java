package com.cuscatlan.coworking.pricing;

import com.cuscatlan.coworking.space.Space;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Tarifa lineal: tarifa/hora * horas. Reglas mas ricas (recargo en hora pico,
 * descuento por duracion) pedirian un Strategy por tipo de espacio; lo dejo como
 * punto de crecimiento, hoy no hay negocio que lo justifique.
 */
@Service
public class PricingService {

    public BigDecimal quote(Space space, Instant start, Instant end) {
        long minutes = Duration.between(start, end).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        return space.getHourlyRate().multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }
}
