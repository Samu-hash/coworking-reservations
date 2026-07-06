package com.cuscatlan.coworking.pricing;

import com.cuscatlan.coworking.space.Space;
import com.cuscatlan.coworking.space.SpaceType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private final PricingService pricing = new PricingService();

    private Space space(String rate) {
        return new Space("Sala", SpaceType.MEETING_ROOM, 8, "Piso 3", new BigDecimal(rate));
    }

    @Test
    void cobra_tarifa_por_horas_completas() {
        Instant start = Instant.parse("2026-07-10T09:00:00Z");
        Instant end = Instant.parse("2026-07-10T11:00:00Z");
        assertThat(pricing.quote(space("10.00"), start, end)).isEqualByComparingTo("20.00");
    }

    @Test
    void prorratea_las_fracciones_de_hora() {
        // 90 minutos a 10/hora = 15.00
        Instant start = Instant.parse("2026-07-10T09:00:00Z");
        Instant end = Instant.parse("2026-07-10T10:30:00Z");
        assertThat(pricing.quote(space("10.00"), start, end)).isEqualByComparingTo("15.00");
    }
}
