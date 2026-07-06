package com.cuscatlan.coworking.reservation;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtro dinamico para el listado de ADMIN. Uso Specifications en vez de un puñado
 * de metodos derivados combinatorios (findByStatus, findByStatusAndSpace, ...).
 */
final class ReservationSpecifications {

    private ReservationSpecifications() {
    }

    static Specification<Reservation> filter(ReservationStatus status, Long spaceId) {
        return (root, query, cb) -> {
            // fetch de space+user solo en la query de datos, no en el count (rompe el count)
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("space", JoinType.LEFT);
                root.fetch("user", JoinType.LEFT);
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (spaceId != null) {
                predicates.add(cb.equal(root.get("space").get("id"), spaceId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
