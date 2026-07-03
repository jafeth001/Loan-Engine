package com.bank.loanengine.auth.controller;

import com.bank.loanengine.auth.dto.AuthService;
import com.bank.loanengine.auth.dto.AuthResponse;
import com.bank.loanengine.auth.dto.LoginRequest;
import com.bank.loanengine.auth.dto.RegisterRequest;
import com.bank.loanengine.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @Operation(
            summary = "Register a new user",
            description = """
                    Creates a new user account with the given username, password and role, then
                    immediately returns a signed JWT so the caller can start making authenticated
                    requests without a separate login step.
                    
                    **Roles:**
                    - `ROLE_ADMIN` — full read/write access to all loan endpoints.
                    - `ROLE_CUSTOMER` — read-only access (GET requests only).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created — JWT returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error (missing/short fields)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Username already taken",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirements   // no JWT needed for this endpoint
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Log in and obtain a JWT",
            description = """
                    Validates the supplied credentials and returns a signed JWT access token.
                    
                    **How to use the token in Swagger UI:**
                    1. Copy the `accessToken` value from the response.
                    2. Click the **Authorize 🔒** button at the top of this page.
                    3. Paste the token (without the "Bearer " prefix) into the `bearerAuth` field.
                    4. Click *Authorize* — all subsequent requests will carry the header automatically.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — JWT returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Bad credentials",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing username or password",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
