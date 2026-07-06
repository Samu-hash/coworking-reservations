package com.cuscatlan.coworking.reservation;

import com.cuscatlan.coworking.reservation.dto.ReservationCreateRequest;
import com.cuscatlan.coworking.reservation.dto.ReservationResponse;
import com.cuscatlan.coworking.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest req,
                                                      @AuthenticationPrincipal AuthenticatedUser caller) {
        ReservationResponse created = service.create(req, caller);
        return ResponseEntity.created(URI.create("/reservations/" + created.id())).body(created);
    }

    @GetMapping("/mine")
    public List<ReservationResponse> mine(@AuthenticationPrincipal AuthenticatedUser caller) {
        return service.listMine(caller);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReservationResponse> all(@RequestParam(required = false) ReservationStatus status,
                                         @RequestParam(required = false) Long spaceId) {
        return service.listAll(status, spaceId);
    }

    @GetMapping("/{id}")
    public ReservationResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser caller) {
        return service.get(id, caller);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ReservationResponse> confirm(@PathVariable Long id,
                                                       @AuthenticationPrincipal AuthenticatedUser caller) {
        ReservationResponse res = service.confirm(id, caller);
        // si el pago quedo pendiente (gateway caido -> fallback) devolvemos 202, no 200
        HttpStatus code = res.status() == ReservationStatus.CONFIRMED ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(code).body(res);
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser caller) {
        service.cancel(id, caller);
    }
}
