package com.cuscatlan.coworking.notification;

import com.cuscatlan.coworking.reservation.event.ReservationConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class ReservationNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationNotificationListener.class);

    // @Async para no bloquear la respuesta HTTP; AFTER_COMMIT para no mandar el
    // correo si la transaccion que confirmo la reserva termino haciendo rollback.
    @Async("notificationsExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onReservationConfirmed(ReservationConfirmedEvent event) {
        // aca iria el envio real; para la prueba lo simulo con un log
        log.info("Enviando correo de confirmacion de la reserva {} a {}",
                event.reservationId(), event.userEmail());
    }
}
