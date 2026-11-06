package app.myfavpodcasts.listening.infrastructure;

import app.myfavpodcasts.listening.domain.EpisodeRef;
import app.myfavpodcasts.listening.domain.ListeningRepository;
import app.myfavpodcasts.listening.domain.QueueItem;
import app.myfavpodcasts.listening.domain.SubscriptionView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ListeningRepositoryAdapter implements ListeningRepository {

    private final SubscriptionJpaRepository subscriptions;
    private final PlaybackJpaRepository playbacks;
    private final QueueJpaRepository queue;

    public ListeningRepositoryAdapter(
            SubscriptionJpaRepository subscriptions,
            PlaybackJpaRepository playbacks,
            QueueJpaRepository queue
    ) {
        this.subscriptions = subscriptions;
        this.playbacks = playbacks;
        this.queue = queue;
    }

    @Override
    public boolean subscriptionExists(UUID userId, UUID podcastId) {
        return subscriptions.existsByUserIdAndPodcastId(userId, podcastId);
    }

    @Override
    @Transactional
    public void follow(UUID userId, UUID podcastId) {
        subscriptions.save(SubscriptionEntity.follow(userId, podcastId));
    }

    @Override
    @Transactional
    public boolean unfollow(UUID userId, UUID podcastId) {
        return subscriptions.deleteByUserIdAndPodcastId(userId, podcastId) > 0;
    }

    @Override
    public List<SubscriptionView> list(UUID userId) {
        return subscriptions.findViews(userId);
    }

    @Override
    public List<UUID> subscribedPodcastIds() {
        return subscriptions.findSubscribedPodcastIds();
    }

    @Override
    public List<QueueItem> items(UUID userId) {
        return queue.findItems(userId);
    }

    @Override
    public Optional<EpisodeRef> episodeForUser(UUID userId, UUID episodeId) {
        return queue.findEpisodeForUser(userId, episodeId);
    }

    @Override
    @Transactional
    public void saveProgress(UUID userId, UUID episodeId, int positionSeconds, boolean completed) {
        PlaybackEntity entity = playbacks.findByUserIdAndEpisodeId(userId, episodeId)
                .orElseGet(() -> PlaybackEntity.start(userId, episodeId));
        entity.apply(positionSeconds, completed);
        playbacks.save(entity);
    }
}
