package app.myfavpodcasts.listening.domain;

import java.time.Instant;
import java.util.UUID;

public record QueueItem(
        UUID episodeId,
        UUID podcastId,
        String podcastTitle,
        String title,
        String summary,
        String audioUrl,
        String artworkUrl,
        Instant publishedAt,
        Integer durationSeconds,
        int positionSeconds,
        boolean completed
) {
}
