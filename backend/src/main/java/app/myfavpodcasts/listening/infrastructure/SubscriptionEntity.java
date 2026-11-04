package app.myfavpodcasts.listening.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "subscriptions",
        uniqueConstraints = @UniqueConstraint(name = "uk_subscription", columnNames = {"userId", "podcastId"}),
        indexes = @Index(name = "idx_subscription_user", columnList = "userId")
)
public class SubscriptionEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID podcastId;

    @Column(nullable = false)
    private Instant createdAt;

    protected SubscriptionEntity() {
    }

    public static SubscriptionEntity follow(UUID userId, UUID podcastId) {
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.id = UUID.randomUUID();
        entity.userId = userId;
        entity.podcastId = podcastId;
        entity.createdAt = Instant.now();
        return entity;
    }
}
