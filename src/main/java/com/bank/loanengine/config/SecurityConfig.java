package com.bank.loanengine.config;

import com.bank.loanengine.security.JwtAccessDeniedHandler;
import com.bank.loanengine.security.JwtAuthEntryPoint;
import com.bank.loanengine.security.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 *
 * <h3>Endpoint access matrix</h3>
 * <pre>
 * POST /api/v1/auth/**             → public (register, login)
 * GET  /api/v1/loans/**            → ADMIN + CUSTOMER (read own schedule)
 * POST /api/v1/loans               → ADMIN only (create loan)
 * POST /api/v1/loans/{id}/prepayments → ADMIN only (apply prepayment)
 * POST /api/v1/loans/{id}/mark-paid-up-to/{n} → ADMIN only (test helper)
 * </pre>
 *
 * Sessions are stateless (JWT-only); CSRF is disabled because there are no browser form
 * submissions in this API.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter  jwtAuthFilter;
    private final UserDetailsService       userDetailsService;
    private final JwtAuthEntryPoint        authEntryPoint;
    private final JwtAccessDeniedHandler   accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          UserDetailsService userDetailsService,
                          JwtAuthEntryPoint authEntryPoint,
                          JwtAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthFilter       = jwtAuthFilter;
        this.userDetailsService  = userDetailsService;
        this.authEntryPoint      = authEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(authEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))

            .authorizeHttpRequests(auth -> auth
                    // Public auth endpoints
                    .requestMatchers("/api/v1/auth/**").permitAll()

                    // Swagger UI + OpenAPI spec (no auth required to read the docs)
                    .requestMatchers(
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs",
                            "/v3/api-docs/**",
                            "/v3/api-docs.yaml"
                    ).permitAll()

                    // H2 console (available only on h2 profile, harmless otherwise)
                    .requestMatchers("/h2-console/**").permitAll()

                    // Loan reads — both roles
                    .requestMatchers(HttpMethod.GET,  "/api/v1/loans/**").hasAnyRole("ADMIN", "CUSTOMER")

                    // Audit — ADMIN only (read-only ledger, sensitive financial data)
                    .requestMatchers(HttpMethod.GET, "/api/v1/audits/**").hasRole("ADMIN")

                    // Loan writes — ADMIN only
                    .requestMatchers(HttpMethod.POST, "/api/v1/loans").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/v1/loans/**").hasRole("ADMIN")

                    // Anything else must be authenticated
                    .anyRequest().authenticated()
            )

            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // Allow H2 console frames when running locally on h2 profile
        http.headers(h -> h.frameOptions(fo -> fo.sameOrigin()));

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
