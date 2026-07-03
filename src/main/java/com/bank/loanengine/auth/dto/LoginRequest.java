package com.bank.loanengine.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials for obtaining a JWT access token")
public record LoginRequest(

        @Schema(description = "Registered username", example = "alice")
        @NotBlank(message = "username is required")
        String username,

        @Schema(description = "Account password", example = "S3cur3Pass!")
        @NotBlank(message = "password is required")
        String password
) {}
