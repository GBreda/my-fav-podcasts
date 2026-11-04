package app.myfavpodcasts.catalog.infrastructure;

import app.myfavpodcasts.catalog.domain.Episode;
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
        name = "episodes",
        uniqueConstraints = @UniqueConstraint(name = "uk_episode_guid", columnNames = {"podcastId", "guid"}),
        indexes = {
                @Index(name = "idx_episode_podcast", columnList = "podcastId"),
                @Index(name = "idx_episode_published", columnList = "publishedAt")
        }
)
public class EpisodeEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID podcastId;

    @Column(nullable = false, length = 500)
    private String guid;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, length = 400)
    private String summary;

    @Column(nullable = false, length = 2000)
    private String audioUrl;

    @Column(nullable = false, length = 1000)
    private String artworkUrl;

    @Column(nullable = false)
    private Instant publishedAt;

    private Integer durationSeconds;

    protected EpisodeEntity() {
    }

    public static EpisodeEntity create(UUID podcastId, String guid) {
        EpisodeEntity entity = new EpisodeEntity();
        entity.id = UUID.randomUUID();
        entity.podcastId = podcastId;
        entity.guid = guid;
        return entity;
    }

    public void apply(String title, String summary, String audioUrl, String artworkUrl, Instant publishedAt, Integer durationSeconds) {
        this.title = title;
        this.summary = summary;
        this.audioUrl = audioUrl;
        this.artworkUrl = artworkUrl;
        this.publishedAt = publishedAt;
        this.durationSeconds = durationSeconds;
    }

    public Episode toDomain() {
        return new Episode(id, podcastId, guid, title, summary, audioUrl, artworkUrl, publishedAt, durationSeconds);
    }

    public UUID getId() {
        return id;
    }
}
