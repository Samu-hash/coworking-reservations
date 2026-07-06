package com.cuscatlan.coworking.report;

import com.cuscatlan.coworking.report.dto.OccupancyReportResponse;
import com.cuscatlan.coworking.report.dto.OccupancyReportResponse.SpaceOccupancy;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class OccupancyReportService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final OccupancyRepository repository;

    public OccupancyReportService(OccupancyRepository repository) {
        this.repository = repository;
    }

    // el reporte es caro si lo pega un dashboard cada pocos segundos y el dato no
    // cambia al minuto, asi que lo cacheo por rango. La invalidacion viaja con las
    // mutaciones de reserva (@CacheEvict en ReservationService).
    @Cacheable(cacheNames = "occupancyReport", key = "#from.toString() + '|' + #to.toString()")
    public OccupancyReportResponse occupancy(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' no puede ser posterior a 'to'");
        }
        // rango [from 00:00, to+1dia 00:00): 'to' inclusivo en dias completos
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<SpaceOccupancy> spaces = repository.occupancyBetween(fromInstant, toInstant).stream()
                .map(OccupancyReportService::toSpaceOccupancy)
                .toList();
        return new OccupancyReportResponse(from, to, spaces);
    }

    private static SpaceOccupancy toSpaceOccupancy(Object[] row) {
        Long spaceId = ((Number) row[0]).longValue();
        String name = (String) row[1];
        BigDecimal reserved = toBigDecimal(row[2]);
        BigDecimal available = toBigDecimal(row[3]);

        BigDecimal pct = BigDecimal.ZERO;
        if (available.signum() > 0) {
            pct = reserved.multiply(HUNDRED).divide(available, 2, RoundingMode.HALF_UP);
            if (pct.compareTo(HUNDRED) > 0) {
                pct = HUNDRED; // clamp: reservas fuera del rango recortadas no deberian pasar de 100
            }
        }
        return new SpaceOccupancy(spaceId, name,
                reserved.setScale(2, RoundingMode.HALF_UP),
                available.setScale(2, RoundingMode.HALF_UP),
                pct);
    }

    private static BigDecimal toBigDecimal(Object value) {
        return value instanceof BigDecimal bd ? bd : BigDecimal.valueOf(((Number) value).doubleValue());
    }
}
