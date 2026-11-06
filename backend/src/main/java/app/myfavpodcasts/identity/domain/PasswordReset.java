package app.myfavpodcasts.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record PasswordReset(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant usedAt) {

    public boolean usable(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public PasswordReset used(Instant now) {
        return new PasswordReset(id, userId, tokenHash, expiresAt, now);
    }
}
