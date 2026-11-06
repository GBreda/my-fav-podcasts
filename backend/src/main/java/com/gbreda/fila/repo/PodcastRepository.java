package com.gbreda.fila.repo;

import com.gbreda.fila.domain.Podcast;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PodcastRepository extends JpaRepository<Podcast, UUID> {

    Optional<Podcast> findByFeedUrl(String feedUrl);

    List<Podcast> findAllByOrderByTitleAsc();
}
