package app.myfavpodcasts.catalog.domain;

import java.time.Instant;
import java.util.UUID;

public record Episode(
        UUID id,
        UUID podcastId,
        String guid,
        String title,
        String summary,
        String audioUrl,
        String artworkUrl,
        Instant publishedAt,
        Integer durationSeconds
) {
}
