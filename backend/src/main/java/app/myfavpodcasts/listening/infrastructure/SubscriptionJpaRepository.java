package app.myfavpodcasts.listening.infrastructure;

import app.myfavpodcasts.listening.domain.SubscriptionView;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, UUID> {

    boolean existsByUserIdAndPodcastId(UUID userId, UUID podcastId);

    long deleteByUserIdAndPodcastId(UUID userId, UUID podcastId);

    @Query("select distinct s.podcastId from SubscriptionEntity s")
    List<UUID> findSubscribedPodcastIds();

    @Query("""
            select new app.myfavpodcasts.listening.domain.SubscriptionView(
                p.id, p.title, p.author, p.description, p.artworkUrl, p.feedUrl)
            from app.myfavpodcasts.catalog.infrastructure.PodcastEntity p
            join SubscriptionEntity s on s.podcastId = p.id
            where s.userId = :userId
            order by s.createdAt desc
            """)
    List<SubscriptionView> findViews(@Param("userId") UUID userId);
}
