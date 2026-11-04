package app.myfavpodcasts.listening.infrastructure;

import app.myfavpodcasts.listening.domain.EpisodeRef;
import app.myfavpodcasts.listening.domain.QueueItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface QueueJpaRepository extends Repository<SubscriptionEntity, UUID> {

    @Query("""
            select new app.myfavpodcasts.listening.domain.QueueItem(
                e.id,
                e.podcastId,
                p.title,
                e.title,
                e.summary,
                e.audioUrl,
                case when e.artworkUrl = '' then p.artworkUrl else e.artworkUrl end,
                e.publishedAt,
                e.durationSeconds,
                coalesce(pb.positionSeconds, 0),
                coalesce(pb.completed, false))
            from app.myfavpodcasts.catalog.infrastructure.EpisodeEntity e
            join app.myfavpodcasts.catalog.infrastructure.PodcastEntity p on p.id = e.podcastId
            join SubscriptionEntity s on s.podcastId = p.id
            left join PlaybackEntity pb on pb.episodeId = e.id and pb.userId = :userId
            where s.userId = :userId
            """)
    List<QueueItem> findItems(@Param("userId") UUID userId);

    @Query("""
            select new app.myfavpodcasts.listening.domain.EpisodeRef(e.id, e.durationSeconds)
            from app.myfavpodcasts.catalog.infrastructure.EpisodeEntity e
            join SubscriptionEntity s on s.podcastId = e.podcastId
            where e.id = :episodeId and s.userId = :userId
            """)
    Optional<EpisodeRef> findEpisodeForUser(@Param("userId") UUID userId, @Param("episodeId") UUID episodeId);
}
