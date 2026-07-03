package com.bank.loanengine.auth.controller;

import com.bank.loanengine.auth.dto.AuthService;
import com.bank.loanengine.auth.dto.AuthResponse;
import com.bank.loanengine.auth.dto.LoginRequest;
import com.bank.loanengine.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Authentication",
        description = "Public endpoints for registering accounts and obtaining JWT tokens. "
                + "No `Authorization` header required."
)
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── Register ─────────────────────────────────────────────────────────────

    @Operation(summary = "Register a new user")
    @SecurityRequirements   // no JWT needed for this endpoint
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Operation(summary = "Log in and obtain a JWT")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
