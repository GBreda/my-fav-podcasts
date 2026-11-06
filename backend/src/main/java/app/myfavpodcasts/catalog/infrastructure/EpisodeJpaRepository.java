package app.myfavpodcasts.catalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeJpaRepository extends JpaRepository<EpisodeEntity, UUID> {

    Optional<EpisodeEntity> findByPodcastIdAndGuid(UUID podcastId, String guid);

    List<EpisodeEntity> findByPodcastIdOrderByPublishedAtDesc(UUID podcastId);
}
