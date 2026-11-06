package app.myfavpodcasts.catalog.domain;

import java.time.Instant;

public record FeedEpisode(
        String guid,
        String title,
        String summary,
        String audioUrl,
        String artworkUrl,
        Instant publishedAt,
        Integer durationSeconds
) {
}
