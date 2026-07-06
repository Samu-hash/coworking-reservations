package com.cuscatlan.coworking.report;

import com.cuscatlan.coworking.reservation.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

interface OccupancyRepository extends JpaRepository<Reservation, Long> {

    /**
     * Ocupacion por espacio en un rango. Es una agregacion en una sola pasada (no
     * materializa entidades, cero N+1). El LEFT JOIN deja los espacios sin reservas
     * con 0. Cada reserva se recorta al rango con least/greatest. Solo cuentan
     * CONFIRMED y COMPLETED: PENDING_PAYMENT es un hold, no ocupacion real.
     * Devuelve [spaceId, name, reservedHours, availableHours].
     */
    // los :from/:to van casteados a timestamptz: si no, Postgres los ve como 'unknown'
    // y la resta :to - :from queda ambigua (operator is not unique)
    @Query(value = """
            select s.id,
                   s.name,
                   coalesce(sum(extract(epoch from (least(r.end_time, cast(:to as timestamptz)) - greatest(r.start_time, cast(:from as timestamptz)))) / 3600.0), 0),
                   extract(epoch from (cast(:to as timestamptz) - cast(:from as timestamptz))) / 3600.0
            from spaces s
            left join reservations r
                   on r.space_id = s.id
                  and r.status in ('CONFIRMED', 'COMPLETED')
                  and tstzrange(r.start_time, r.end_time, '[)') && tstzrange(cast(:from as timestamptz), cast(:to as timestamptz), '[)')
            where s.active = true
            group by s.id, s.name
            order by s.name
            """, nativeQuery = true)
    List<Object[]> occupancyBetween(@Param("from") Instant from, @Param("to") Instant to);
}
