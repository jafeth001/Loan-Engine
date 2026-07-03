package com.bank.loanengine.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Creates and validates HS256-signed JSON Web Tokens.
 *
 * <p>The signing secret must be a Base64-encoded string of at least 32 random bytes set in
 * {@code application.yml} under {@code app.jwt.secret}.  Generate one with:
 * <pre>openssl rand -base64 64</pre>
 *
 * <p>Access tokens are short-lived (default 1 hour); set {@code app.jwt.expiration-ms} to
 * adjust.  Refresh tokens are out of scope for this assessment.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String CLAIM_ROLE = "role";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Token generation ─────────────────────────────────────────────────────────────────────

    /**
     * Generates a signed JWT for an authenticated principal.
     *
     * @param authentication Spring Security authentication object (principal must be a UserDetails)
     * @return compact serialized JWT string
     */
    public String generateToken(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        return buildToken(principal.getUsername(),
                principal.getAuthorities().stream().findFirst()
                        .map(a -> a.getAuthority()).orElse(""));
    }

    /**
     * Generates a signed JWT directly from a username and role string. Used after registration
     * so the caller gets a token without a round-trip authentication object.
     */
    public String generateToken(String username, String role) {
        return buildToken(username, role);
    }

    private String buildToken(String username, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    // ── Token validation ─────────────────────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT unsupported: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("JWT malformed: {}", ex.getMessage());
        } catch (io.jsonwebtoken.security.SecurityException ex) {
            log.warn("JWT signature invalid: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims empty: {}", ex.getMessage());
        }
        return false;
    }

    // ── Claims extraction ────────────────────────────────────────────────────────────────────

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
