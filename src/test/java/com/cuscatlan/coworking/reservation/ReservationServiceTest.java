package com.cuscatlan.coworking.reservation;

import com.cuscatlan.coworking.payment.PaymentGatewayClient;
import com.cuscatlan.coworking.payment.PaymentValidationResult;
import com.cuscatlan.coworking.pricing.PricingService;
import com.cuscatlan.coworking.reservation.dto.ReservationCreateRequest;
import com.cuscatlan.coworking.reservation.event.ReservationConfirmedEvent;
import com.cuscatlan.coworking.reservation.exception.OverlappingReservationException;
import com.cuscatlan.coworking.security.AuthenticatedUser;
import com.cuscatlan.coworking.space.Space;
import com.cuscatlan.coworking.space.SpaceType;
import com.cuscatlan.coworking.user.Role;
import com.cuscatlan.coworking.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Instant START = Instant.parse("2026-07-10T09:00:00Z");
    private static final Instant END = Instant.parse("2026-07-10T10:00:00Z");

    @Mock
    private ReservationRepository reservations;
    @Mock
    private com.cuscatlan.coworking.space.SpaceService spaceService;
    @Mock
    private com.cuscatlan.coworking.user.UserRepository users;
    @Mock
    private PaymentGatewayClient paymentGateway;
    @Mock
    private ReservationMapper mapper;
    @Mock
    private ApplicationEventPublisher events;
    @Mock
    private PlatformTransactionManager txManager;

    private final PricingService pricing = new PricingService();

    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService(reservations, spaceService, users, pricing,
                paymentGateway, mapper, events, txManager);
        // que el TransactionTemplate ejecute el callback sin una transaccion real
        lenient().when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
    }

    @Test
    void rechaza_la_creacion_si_ya_hay_una_reserva_solapada() {
        when(spaceService.getEntity(5L)).thenReturn(activeSpace());
        when(reservations.existsOverlap(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ReservationCreateRequest(5L, START, END), admin()))
                .isInstanceOf(OverlappingReservationException.class);

        verify(reservations, never()).saveAndFlush(any());
    }

    @Test
    void al_confirmar_con_pago_aprobado_queda_confirmed_y_publica_el_evento() {
        Reservation reservation = pendingReservation();
        when(reservations.findById(10L)).thenReturn(Optional.of(reservation));
        when(paymentGateway.validate(any())).thenReturn(new PaymentValidationResult("APPROVED", "PMT-1"));

        service.confirm(10L, admin());

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getPaymentRef()).isEqualTo("PMT-1");
        verify(events).publishEvent(any(ReservationConfirmedEvent.class));
    }

    @Test
    void si_el_pago_cae_al_fallback_la_reserva_sigue_pendiente_y_no_notifica() {
        Reservation reservation = pendingReservation();
        when(reservations.findById(10L)).thenReturn(Optional.of(reservation));
        when(paymentGateway.validate(any())).thenReturn(PaymentValidationResult.pending());

        service.confirm(10L, admin());

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        verify(events, never()).publishEvent(any());
    }

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, "admin", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private Space activeSpace() {
        return new Space("Sala", SpaceType.MEETING_ROOM, 8, "Piso 3", new BigDecimal("10.00"));
    }

    private Reservation pendingReservation() {
        User user = new User("ana@test.sv", "hash", "Ana", Role.USER);
        return new Reservation(activeSpace(), user, START, END, new BigDecimal("10.00"));
    }
}
