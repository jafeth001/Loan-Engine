package com.bank.loanengine.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures the springdoc-openapi {@link OpenAPI} bean that drives the Swagger UI and the
 * machine-readable spec at {@code /v3/api-docs}.
 *
 * <h3>Swagger UI URLs</h3>
 * <ul>
 *   <li>{@code http://localhost:8080/swagger-ui.html}  — interactive HTML UI</li>
 *   <li>{@code http://localhost:8080/v3/api-docs}       — raw OpenAPI 3.1 JSON</li>
 *   <li>{@code http://localhost:8080/v3/api-docs.yaml}  — raw OpenAPI 3.1 YAML</li>
 * </ul>
 *
 * <h3>Authentication in Swagger UI</h3>
 * <ol>
 *   <li>Call {@code POST /api/v1/auth/register} or {@code POST /api/v1/auth/login}.</li>
 *   <li>Copy the {@code accessToken} from the response.</li>
 *   <li>Click the <strong>Authorize 🔒</strong> button at the top of the UI.</li>
 *   <li>Paste the token (without "Bearer ") into the {@code bearerAuth} field and click
 *       <em>Authorize</em>.</li>
 *   <li>All subsequent requests will include {@code Authorization: Bearer <token>}.</li>
 * </ol>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI loanEngineOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server"),
                        new Server()
                                .url("http://app:8080")
                                .description("Docker Compose internal network")
                ))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()))
                // Apply the bearer-auth security globally so every protected endpoint
                // shows the padlock icon in Swagger UI without repeating @SecurityRequirement
                // on every method. The auth endpoints override this with @SecurityRequirement({}).
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .externalDocs(new ExternalDocumentation()
                        .description("Assessment brief & sample loan data")
                        .url("https://docs.google.com/spreadsheets/d/16olhrtNe8bnrZ9e7AItBxrbjBwQQJxaMO3BGbRyIDU"));
    }

    private Info apiInfo() {
        return new Info()
                .title("Loan Settlement & Prepayment Engine API")
                .version("1.0.0")
                .description("""
                        ## Core Banking Loan Prepayment Engine
                        
                        Implements **Category A — Prepayment of Principal** with three business strategies:
                        
                        | Option | Strategy | Behaviour |
                        |--------|----------|-----------|
                        | **A** | `REDUCE_EMI_KEEP_TENOR` | Principal drops; tenor fixed; EMI recalculated downward (~15,700) |
                        | **B** | `REDUCE_TENOR_KEEP_EMI` | Principal drops; EMI fixed; tenor shortened (~22 months) |
                        | **C** | `ADVANCE_INSTALLMENTS` | Prepayment treated as a pool of future EMIs; no recalculation |
                        
                        ### Base loan assumptions (from assessment brief)
                        - Principal: **1,000,000** · Rate: **12% p.a.** · Tenor: **60 months** · EMI ≈ **22,244**
                        - Outstanding principal after month 24 ≈ **680,000**
                        - Prepayment at month 24 with **200,000**
                        
                        ### Authentication
                        All endpoints except `/api/v1/auth/**` require a JWT.  
                        Register or login, then click **Authorize 🔒** and paste the `accessToken`.
                        
                        ### Role-based access
                        | Role | Can do |
                        |------|--------|
                        | `ROLE_ADMIN` | All operations |
                        | `ROLE_CUSTOMER` | Read loan & schedule (GET only) |
                        """)
                .contact(new Contact()
                        .name("Loan Engine Team")
                        .email("dev@loan-engine.example.com"))
                .license(new License()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name(SECURITY_SCHEME_NAME)
                .description("""
                        Enter the JWT token obtained from `POST /api/v1/auth/login`.
                        
                        Example: `eyJhbGciOiJIUzI1NiJ9...`
                        
                        Do **not** prefix with "Bearer " — springdoc adds it automatically.
                        """);
    }
}
