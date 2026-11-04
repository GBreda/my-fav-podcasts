package app.myfavpodcasts.listening.domain;

import java.util.UUID;

public record SubscriptionView(
        UUID id,
        String title,
        String author,
        String description,
        String artworkUrl,
        String feedUrl
) {
}
