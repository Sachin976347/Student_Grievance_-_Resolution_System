package com.grievance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT utility — upgraded for JJWT 0.12.x (jakarta.* namespace).
 *
 * Key API changes from 0.11.x → 0.12.x:
 *   - Jwts.builder() chain is the same but .signWith() no longer takes
 *     a SignatureAlgorithm argument; the key type implies the algorithm.
 *   - Jwts.parserBuilder() renamed to Jwts.parser()
 *   - .parseClaimsJws() renamed to .parseSignedClaims()
 *   - Keys.hmacShaKeyFor() still works the same way.
 */
@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ─── Build the signing key ────────────────────────────────────────────────
    private SecretKey signingKey() {
        // Secret must be >= 256 bits (32 bytes) for HS256
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // ─── Generate token from authenticated principal ───────────────────────────
    public String generateToken(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .subject(principal.getUsername())          // 0.12.x: .subject() not .setSubject()
                .issuedAt(new Date())                      // 0.12.x: .issuedAt()
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey())                    // 0.12.x: no algorithm arg needed
                .compact();
    }

    // ─── Extract username from token ──────────────────────────────────────────
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()                      // 0.12.x: parser() not parserBuilder()
                .verifyWith(signingKey())                  // 0.12.x: verifyWith() not setSigningKey()
                .build()
                .parseSignedClaims(token)                  // 0.12.x: parseSignedClaims() not parseClaimsJws()
                .getPayload();
        return claims.getSubject();
    }

    // ─── Validate token ───────────────────────────────────────────────────────
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("[JwtUtils] Invalid JWT token: " + e.getMessage());
            return false;
        }
    }
}