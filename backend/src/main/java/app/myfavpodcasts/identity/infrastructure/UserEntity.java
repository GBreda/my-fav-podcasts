package app.myfavpodcasts.identity.infrastructure;

import app.myfavpodcasts.identity.domain.DisplayName;
import app.myfavpodcasts.identity.domain.Email;
import app.myfavpodcasts.identity.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 40)
    private String displayName;

    @Column(nullable = false)
    private Instant createdAt;

    protected UserEntity() {
    }

    public static UserEntity from(User user) {
        UserEntity entity = new UserEntity();
        entity.id = user.id();
        entity.email = user.email().value();
        entity.passwordHash = user.passwordHash();
        entity.displayName = user.displayName().value();
        entity.createdAt = user.createdAt();
        return entity;
    }

    public void applyPassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public User toDomain() {
        return new User(id, new Email(email), passwordHash, new DisplayName(displayName), createdAt);
    }

    public UUID getId() {
        return id;
    }
}
