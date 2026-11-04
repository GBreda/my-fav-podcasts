package app.myfavpodcasts.shared.security;

import app.myfavpodcasts.shared.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(AppProperties properties) {
        byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.ttlSeconds = properties.jwt().ttlDays() * 24L * 60L * 60L;
    }

    public String issue(UUID userId, String email, String displayName) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("name", displayName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    public Optional<AuthPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            UUID id = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            String name = claims.get("name", String.class);
            if (email == null || name == null) {
                return Optional.empty();
            }
            return Optional.of(new AuthPrincipal(id, email, name));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
