package app.myfavpodcasts.catalog.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record Podcast(
        UUID id,
        String feedUrl,
        String title,
        String author,
        String description,
        String artworkUrl,
        Instant lastSyncedAt
) {

    public boolean isFresh(Duration interval) {
        return lastSyncedAt != null && lastSyncedAt.isAfter(Instant.now().minus(interval));
    }
}
