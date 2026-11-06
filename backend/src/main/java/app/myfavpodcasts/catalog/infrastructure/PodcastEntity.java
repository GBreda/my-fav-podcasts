package app.myfavpodcasts.catalog.infrastructure;

import app.myfavpodcasts.catalog.domain.Podcast;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "podcasts")
public class PodcastEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 1000)
    private String feedUrl;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, length = 160)
    private String author;

    @Column(nullable = false, length = 1200)
    private String description;

    @Column(nullable = false, length = 1000)
    private String artworkUrl;

    private Instant lastSyncedAt;

    protected PodcastEntity() {
    }

    public static PodcastEntity create(UUID id, String feedUrl) {
        PodcastEntity entity = new PodcastEntity();
        entity.id = id;
        entity.feedUrl = feedUrl;
        entity.title = "";
        entity.author = "";
        entity.description = "";
        entity.artworkUrl = "";
        return entity;
    }

    public void apply(String title, String author, String description, String artworkUrl, Instant syncedAt) {
        this.title = title;
        this.author = author;
        this.description = description;
        this.artworkUrl = artworkUrl;
        this.lastSyncedAt = syncedAt;
    }

    public Podcast toDomain() {
        return new Podcast(id, feedUrl, title, author, description, artworkUrl, lastSyncedAt);
    }

    public UUID getId() {
        return id;
    }
}
