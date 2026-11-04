package app.myfavpodcasts.catalog.infrastructure;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PodcastJpaRepository extends JpaRepository<PodcastEntity, UUID> {

    Optional<PodcastEntity> findByFeedUrl(String feedUrl);

    @Query("""
            select p from PodcastEntity p
            where p.id in :ids
              and (p.lastSyncedAt is null or p.lastSyncedAt < :cutoff)
            """)
    List<PodcastEntity> findStaleAmong(@Param("ids") Collection<UUID> ids, @Param("cutoff") Instant cutoff);
}
