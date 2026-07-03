package com.bank.loanengine.auth.service;

import com.bank.loanengine.auth.domain.AppUser;
import com.bank.loanengine.auth.domain.AppUserRepository;
import com.bank.loanengine.auth.dto.AuthResponse;
import com.bank.loanengine.auth.dto.LoginRequest;
import com.bank.loanengine.auth.dto.RegisterRequest;
import com.bank.loanengine.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository    userRepository;
    private final PasswordEncoder      passwordEncoder;
    private final JwtTokenProvider     jwtTokenProvider;
    private final AuthenticationManager authManager;

    /**
     * Creates a new user account and immediately returns a JWT so the caller can start
     * making authenticated requests without a separate login step.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException(
                    "Username '" + request.username() + "' is already taken.");
        }

        AppUser user = AppUser.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        AppUser saved = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(saved.getUsername(), saved.getRole().name());
        return AuthResponse.of(token,
                jwtTokenProvider.getExpirationMs() / 1000,
                saved.getId(), saved.getUsername(), saved.getRole());
    }

    /**
     * Authenticates credentials and returns a JWT.
     *
     * @throws AuthenticationException if credentials are wrong (Spring maps to 401).
     */
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        AppUser user = (AppUser) auth.getPrincipal();
        String token = jwtTokenProvider.generateToken(auth);

        return AuthResponse.of(token,
                jwtTokenProvider.getExpirationMs() / 1000,
                user.getId(), user.getUsername(), user.getRole());
    }
}
