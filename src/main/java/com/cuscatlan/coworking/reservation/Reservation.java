package com.cuscatlan.coworking.reservation;

import com.cuscatlan.coworking.common.domain.BaseAuditEntity;
import com.cuscatlan.coworking.reservation.exception.IllegalReservationTransitionException;
import com.cuscatlan.coworking.space.Space;
import com.cuscatlan.coworking.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "ix_res_user_start", columnList = "user_id, start_time"),
        @Index(name = "ix_res_status", columnList = "status")
})
public class Reservation extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY explicito: el default de @ManyToOne es EAGER y es la fuente #1 de N+1 al listar
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false, foreignKey = @ForeignKey(name = "fk_res_space"))
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_res_user"))
    private User user;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "price_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceTotal;

    @Column(name = "payment_ref", length = 64)
    private String paymentRef;

    @Version
    private Long version;

    protected Reservation() {
    }

    public Reservation(Space space, User user, Instant startTime, Instant endTime, BigDecimal priceTotal) {
        this.space = space;
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
        this.priceTotal = priceTotal;
        this.status = ReservationStatus.PENDING_PAYMENT;
    }

    public void transitionTo(ReservationStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalReservationTransitionException(status, target);
        }
        this.status = target;
    }

    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }

    public Long getId() {
        return id;
    }

    public Space getSpace() {
        return space;
    }

    public User getUser() {
        return user;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public BigDecimal getPriceTotal() {
        return priceTotal;
    }

    public String getPaymentRef() {
        return paymentRef;
    }
}
