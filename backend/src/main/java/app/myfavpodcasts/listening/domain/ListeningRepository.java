package app.myfavpodcasts.listening.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListeningRepository {

    boolean subscriptionExists(UUID userId, UUID podcastId);

    void follow(UUID userId, UUID podcastId);

    boolean unfollow(UUID userId, UUID podcastId);

    List<SubscriptionView> list(UUID userId);

    List<UUID> subscribedPodcastIds();

    List<QueueItem> items(UUID userId);

    Optional<EpisodeRef> episodeForUser(UUID userId, UUID episodeId);

    void saveProgress(UUID userId, UUID episodeId, int positionSeconds, boolean completed);
}
