package com.cuscatlan.coworking.reservation;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de la reserva como maquina de estados. Cada valor declara a que
 * estados puede saltar; la regla vive pegada al estado, no en un switch regado por
 * el service. CANCELLED y COMPLETED son terminales.
 */
public enum ReservationStatus {

    PENDING_PAYMENT {
        @Override
        public Set<ReservationStatus> next() {
            return EnumSet.of(CONFIRMED, CANCELLED);
        }
    },
    CONFIRMED {
        @Override
        public Set<ReservationStatus> next() {
            return EnumSet.of(COMPLETED, CANCELLED);
        }
    },
    CANCELLED {
        @Override
        public Set<ReservationStatus> next() {
            return EnumSet.noneOf(ReservationStatus.class);
        }
    },
    COMPLETED {
        @Override
        public Set<ReservationStatus> next() {
            return EnumSet.noneOf(ReservationStatus.class);
        }
    };

    public abstract Set<ReservationStatus> next();

    public boolean canTransitionTo(ReservationStatus target) {
        return next().contains(target);
    }
}
