package app.myfavpodcasts.catalog.domain;

import java.util.List;

public record FeedSnapshot(
        String title,
        String author,
        String description,
        String artworkUrl,
        List<FeedEpisode> episodes
) {
}
