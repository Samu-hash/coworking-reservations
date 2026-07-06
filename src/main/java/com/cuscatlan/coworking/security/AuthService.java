package com.cuscatlan.coworking.security;

import com.cuscatlan.coworking.security.dto.AuthResponse;
import com.cuscatlan.coworking.security.dto.LoginRequest;
import com.cuscatlan.coworking.security.dto.RegisterRequest;
import com.cuscatlan.coworking.user.Role;
import com.cuscatlan.coworking.user.User;
import com.cuscatlan.coworking.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public void register(RegisterRequest req) {
        String email = req.email().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya esta registrado");
        }
        // el registro abierto solo crea USER; los ADMIN se siembran o los crea otro admin
        users.save(new User(email, encoder.encode(req.password()), req.fullName(), Role.USER));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }
        return new AuthResponse(jwt.generate(user), "Bearer", jwt.accessTtlSeconds());
    }
}
