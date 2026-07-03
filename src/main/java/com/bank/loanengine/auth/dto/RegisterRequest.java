package com.bank.loanengine.auth.dto;

import com.bank.loanengine.auth.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for creating a new user account")
public record RegisterRequest(

        @Schema(description = "Unique login name", example = "alice", minLength = 3, maxLength = 80)
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 80, message = "username must be 3-80 characters")
        String username,

        @Schema(description = "Password (min 8 characters, stored BCrypt-hashed)", example = "S3cur3Pass!", minLength = 8)
        @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password,

        @Schema(description = "Granted role. ROLE_ADMIN has full write access; ROLE_CUSTOMER is read-only.",
                example = "ROLE_ADMIN", allowableValues = {"ROLE_ADMIN", "ROLE_CUSTOMER"})
        @NotNull(message = "role is required (ROLE_ADMIN or ROLE_CUSTOMER)")
        Role role
) {}
