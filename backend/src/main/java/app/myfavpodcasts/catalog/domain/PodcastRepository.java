package app.myfavpodcasts.catalog.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PodcastRepository {

    Optional<Podcast> findById(UUID id);

    Optional<Podcast> findByFeedUrl(String feedUrl);

    List<Episode> episodes(UUID podcastId);

    Podcast saveSynced(Podcast current, String feedUrl, PodcastDraft draft, FeedSnapshot snapshot, int maxEpisodes);

    List<Podcast> findStaleAmong(Collection<UUID> ids, Instant cutoff);
}
