package com.cuscatlan.coworking.reservation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>,
        JpaSpecificationExecutor<Reservation> {

    @Override
    @EntityGraph(attributePaths = {"space", "user"})
    Optional<Reservation> findById(Long id);

    @EntityGraph(attributePaths = {"space"})
    List<Reservation> findByUserIdOrderByStartTimeDesc(Long userId);

    // pre-check para dar un 409 amable en el caso comun. NO es la garantia de
    // consistencia (puede dar falso negativo bajo carrera); esa la da la exclusion
    // constraint. El predicado de solape clasico: startA < endB and endA > startB.
    @Query("""
            select (count(r) > 0) from Reservation r
            where r.space.id = :spaceId
              and r.status in (com.cuscatlan.coworking.reservation.ReservationStatus.PENDING_PAYMENT,
                               com.cuscatlan.coworking.reservation.ReservationStatus.CONFIRMED)
              and r.startTime < :end
              and r.endTime > :start
            """)
    boolean existsOverlap(@Param("spaceId") Long spaceId,
                          @Param("start") Instant start,
                          @Param("end") Instant end);
}
