package com.cuscatlan.coworking.reservation;

import com.cuscatlan.coworking.payment.PaymentGatewayClient;
import com.cuscatlan.coworking.payment.PaymentRequest;
import com.cuscatlan.coworking.payment.PaymentValidationResult;
import com.cuscatlan.coworking.pricing.PricingService;
import com.cuscatlan.coworking.reservation.dto.ReservationCreateRequest;
import com.cuscatlan.coworking.reservation.dto.ReservationResponse;
import com.cuscatlan.coworking.reservation.event.ReservationConfirmedEvent;
import com.cuscatlan.coworking.reservation.exception.OverlappingReservationException;
import com.cuscatlan.coworking.security.AuthenticatedUser;
import com.cuscatlan.coworking.space.Space;
import com.cuscatlan.coworking.space.SpaceService;
import com.cuscatlan.coworking.user.User;
import com.cuscatlan.coworking.user.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservations;
    private final SpaceService spaceService;
    private final UserRepository users;
    private final PricingService pricing;
    private final PaymentGatewayClient paymentGateway;
    private final ReservationMapper mapper;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate txTemplate;

    public ReservationService(ReservationRepository reservations, SpaceService spaceService,
                              UserRepository users, PricingService pricing,
                              PaymentGatewayClient paymentGateway, ReservationMapper mapper,
                              ApplicationEventPublisher events, PlatformTransactionManager txManager) {
        this.reservations = reservations;
        this.spaceService = spaceService;
        this.users = users;
        this.pricing = pricing;
        this.paymentGateway = paymentGateway;
        this.mapper = mapper;
        this.events = events;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Transactional
    public ReservationResponse create(ReservationCreateRequest req, AuthenticatedUser caller) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora de fin debe ser posterior al inicio");
        }
        Space space = spaceService.getEntity(req.spaceId());
        if (!space.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El espacio no esta disponible para reservas");
        }
        if (reservations.existsOverlap(space.getId(), req.startTime(), req.endTime())) {
            throw new OverlappingReservationException(space.getId(), req.startTime(), req.endTime());
        }

        BigDecimal price = pricing.quote(space, req.startTime(), req.endTime());
        User user = users.getReferenceById(caller.id());
        Reservation reservation = new Reservation(space, user, req.startTime(), req.endTime(), price);

        try {
            // flush explicito: sin esto el insert se dispara al cerrar la transaccion y el
            // catch no lo veria. Asi la violacion de la constraint ocurre dentro del try.
            reservations.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException ex) {
            // carrera que paso el pre-check: la exclusion constraint la corto (23P01).
            // Ambos caminos terminan en el mismo 409, el usuario nunca ve un 500.
            if (isExclusionViolation(ex)) {
                throw new OverlappingReservationException(space.getId(), req.startTime(), req.endTime());
            }
            throw ex;
        }
        return mapper.toResponse(reservation);
    }

    private boolean isExclusionViolation(DataIntegrityViolationException ex) {
        return ex.getMostSpecificCause() instanceof SQLException sql && "23P01".equals(sql.getSQLState());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listMine(AuthenticatedUser caller) {
        return reservations.findByUserIdOrderByStartTimeDesc(caller.id()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listAll(ReservationStatus status, Long spaceId) {
        return reservations.findAll(ReservationSpecifications.filter(status, spaceId)).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(Long id, AuthenticatedUser caller) {
        Reservation reservation = load(id);
        ensureVisible(reservation, caller);
        return mapper.toResponse(reservation);
    }

    @Transactional
    @CacheEvict(cacheNames = "occupancyReport", allEntries = true)
    public void cancel(Long id, AuthenticatedUser caller) {
        Reservation reservation = load(id);
        ensureVisible(reservation, caller);
        reservation.transitionTo(ReservationStatus.CANCELLED);
    }

    /**
     * Confirma la reserva validando el pago contra el servicio externo. La llamada al
     * gateway (lento/inestable) va FUERA de cualquier transaccion de BD para no tener
     * una conexion abierta durante el HTTP; el resultado se aplica en una tx corta.
     */
    @CacheEvict(cacheNames = "occupancyReport", allEntries = true)
    public ReservationResponse confirm(Long id, AuthenticatedUser caller) {
        Reservation reservation = load(id);
        ensureVisible(reservation, caller);
        if (reservation.getStatus() != ReservationStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva no esta pendiente de pago");
        }

        PaymentValidationResult result = paymentGateway.validate(
                new PaymentRequest(reservation.getId(), reservation.getPriceTotal()));

        // FIXME el chequeo de estado de arriba es fuera de la tx; dos confirm concurrentes
        // pueden colarse hasta aca y chocar en el @Version. Hoy sale un 409 (optimistic lock),
        // que es aceptable, pero lo prolijo seria recargar y revalidar el estado dentro de la tx.
        return txTemplate.execute(status -> applyPaymentOutcome(id, result));
    }

    private ReservationResponse applyPaymentOutcome(Long id, PaymentValidationResult result) {
        Reservation reservation = load(id);
        if (result.approved()) {
            reservation.setPaymentRef(result.reference());
            reservation.transitionTo(ReservationStatus.CONFIRMED);
            // el evento dispara la notificacion async (y viaja AFTER_COMMIT); si el pago
            // no se aprobo no se publica nada, ni correo ni nada
            events.publishEvent(new ReservationConfirmedEvent(
                    reservation.getId(), reservation.getSpace().getId(), reservation.getUser().getEmail()));
        }
        // si no fue aprobado (o cayo el fallback), se queda en PENDING_PAYMENT
        return mapper.toResponse(reservation);
    }

    // TODO nadie pasa a COMPLETED las reservas ya vencidas; falta un job programado
    private Reservation load(Long id) {
        return reservations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada: " + id));
    }

    // ownership: un USER solo ve las suyas. Devuelvo 404 (no 403) para no revelar
    // que existe una reserva ajena con ese id.
    private void ensureVisible(Reservation reservation, AuthenticatedUser caller) {
        if (!caller.isAdmin() && !reservation.getUser().getId().equals(caller.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada: " + reservation.getId());
        }
    }
}
