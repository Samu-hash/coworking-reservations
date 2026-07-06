package com.cuscatlan.coworking.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Principal que queda en el SecurityContext. Lleva el id del usuario ({@code uid})
 * para poder resolver ownership (que un USER solo toque sus reservas) sin ir a la base
 * en cada request.
 */
public record AuthenticatedUser(Long id, String username, Collection<? extends GrantedAuthority> authorities) {

    public boolean isAdmin() {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
