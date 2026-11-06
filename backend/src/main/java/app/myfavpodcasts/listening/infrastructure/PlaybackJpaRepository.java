package app.myfavpodcasts.listening.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaybackJpaRepository extends JpaRepository<PlaybackEntity, UUID> {

    Optional<PlaybackEntity> findByUserIdAndEpisodeId(UUID userId, UUID episodeId);
}
