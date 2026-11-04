package app.myfavpodcasts.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record User(UUID id, Email email, String passwordHash, DisplayName displayName, Instant createdAt) {

    public static User register(Email email, DisplayName displayName, String passwordHash) {
        return new User(UUID.randomUUID(), email, passwordHash, displayName, Instant.now());
    }

    public User withPassword(String passwordHash) {
        return new User(id, email, passwordHash, displayName, createdAt);
    }
}
