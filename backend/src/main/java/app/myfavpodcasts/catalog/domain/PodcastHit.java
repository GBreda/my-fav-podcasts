package app.myfavpodcasts.catalog.domain;

public record PodcastHit(
        String itunesId,
        String title,
        String author,
        String artworkUrl,
        String feedUrl,
        String genre
) {
}
