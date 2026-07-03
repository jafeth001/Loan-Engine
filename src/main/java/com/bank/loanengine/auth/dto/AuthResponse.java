package com.bank.loanengine.auth.dto;

import com.bank.loanengine.auth.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT token response returned after successful register or login")
public record AuthResponse(

        @Schema(description = "Signed JWT access token — pass as Authorization: Bearer <token>",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSJ9.xxxx")
        String accessToken,

        @Schema(description = "Token type, always Bearer", example = "Bearer")
        String tokenType,

        @Schema(description = "Seconds until the token expires", example = "3600")
        long expiresInSeconds,

        @Schema(description = "Persisted user ID", example = "1")
        Long userId,

        @Schema(description = "Username of the authenticated user", example = "alice")
        String username,

        @Schema(description = "Granted role", example = "ROLE_ADMIN")
        Role role
) {
    public static AuthResponse of(String token, long expiresIn, Long userId, String username, Role role) {
        return new AuthResponse(token, "Bearer", expiresIn, userId, username, role);
    }
}
