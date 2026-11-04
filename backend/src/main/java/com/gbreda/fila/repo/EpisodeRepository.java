package com.gbreda.fila.repo;

import com.gbreda.fila.domain.Episode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EpisodeRepository extends JpaRepository<Episode, UUID> {

    long countByPodcastId(UUID podcastId);

    long countByPodcastIdAndPlayedFalse(UUID podcastId);

    long countByPlayedFalse();

    List<Episode> findByPodcastIdOrderByPublishedAtDesc(UUID podcastId);

    Optional<Episode> findByPodcastIdAndGuid(UUID podcastId, String guid);

    @Query("""
            select e.id from Episode e
            where e.played = false
            order by e.publishedAt desc nulls last, e.title asc
            """)
    List<UUID> findUnplayedNewestIds(Pageable pageable);

    @Query("""
            select e.id from Episode e
            where e.played = false
            order by e.publishedAt asc nulls last, e.title asc
            """)
    List<UUID> findUnplayedOldestIds(Pageable pageable);

    @Query("""
            select e from Episode e
            join fetch e.podcast
            where e.id in :ids
            """)
    List<Episode> findAllWithPodcast(@Param("ids") Collection<UUID> ids);

    @Query("""
            select e from Episode e
            join fetch e.podcast
            where e.id = :id
            """)
    Optional<Episode> findWithPodcast(@Param("id") UUID id);
}
