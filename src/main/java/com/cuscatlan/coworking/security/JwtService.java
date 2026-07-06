package com.cuscatlan.coworking.security;

import com.cuscatlan.coworking.config.properties.JwtProperties;
import com.cuscatlan.coworking.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generate(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("roles", List.of("ROLE_" + user.getRole().name()))
                .issuer(props.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessTtl())))
                .signWith(key)
                .compact();
    }

    @SuppressWarnings("unchecked")
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long uid = claims.get("uid", Long.class);
        List<String> roles = claims.get("roles", List.class);
        var authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();
        return new AuthenticatedUser(uid, claims.getSubject(), authorities);
    }

    public long accessTtlSeconds() {
        return props.accessTtl().toSeconds();
    }
}
