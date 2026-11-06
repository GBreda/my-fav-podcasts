package app.myfavpodcasts.identity.infrastructure;

import app.myfavpodcasts.identity.domain.PasswordReset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_resets")
public class PasswordResetEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    protected PasswordResetEntity() {
    }

    public static PasswordResetEntity from(PasswordReset reset) {
        PasswordResetEntity entity = new PasswordResetEntity();
        entity.id = reset.id();
        entity.userId = reset.userId();
        entity.tokenHash = reset.tokenHash();
        entity.expiresAt = reset.expiresAt();
        entity.usedAt = reset.usedAt();
        return entity;
    }

    public void apply(PasswordReset reset) {
        this.userId = reset.userId();
        this.tokenHash = reset.tokenHash();
        this.expiresAt = reset.expiresAt();
        this.usedAt = reset.usedAt();
    }

    public PasswordReset toDomain() {
        return new PasswordReset(id, userId, tokenHash, expiresAt, usedAt);
    }
}
