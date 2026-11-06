package app.myfavpodcasts.listening.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "playback",
        uniqueConstraints = @UniqueConstraint(name = "uk_playback", columnNames = {"userId", "episodeId"})
)
public class PlaybackEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID episodeId;

    @Column(nullable = false)
    private int positionSeconds;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PlaybackEntity() {
    }

    public static PlaybackEntity start(UUID userId, UUID episodeId) {
        PlaybackEntity entity = new PlaybackEntity();
        entity.id = UUID.randomUUID();
        entity.userId = userId;
        entity.episodeId = episodeId;
        return entity;
    }

    public void apply(int positionSeconds, boolean completed) {
        this.positionSeconds = positionSeconds;
        this.completed = completed;
        this.updatedAt = Instant.now();
    }
}
