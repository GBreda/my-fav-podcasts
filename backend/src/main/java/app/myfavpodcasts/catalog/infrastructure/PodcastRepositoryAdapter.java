package app.myfavpodcasts.catalog.infrastructure;

import app.myfavpodcasts.catalog.domain.Episode;
import app.myfavpodcasts.catalog.domain.FeedEpisode;
import app.myfavpodcasts.catalog.domain.FeedSnapshot;
import app.myfavpodcasts.catalog.domain.Podcast;
import app.myfavpodcasts.catalog.domain.PodcastDraft;
import app.myfavpodcasts.catalog.domain.PodcastRepository;
import app.myfavpodcasts.shared.text.PlainText;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PodcastRepositoryAdapter implements PodcastRepository {

    private final PodcastJpaRepository podcasts;
    private final EpisodeJpaRepository episodes;

    public PodcastRepositoryAdapter(PodcastJpaRepository podcasts, EpisodeJpaRepository episodes) {
        this.podcasts = podcasts;
        this.episodes = episodes;
    }

    @Override
    public Optional<Podcast> findById(UUID id) {
        return podcasts.findById(id).map(PodcastEntity::toDomain);
    }

    @Override
    public Optional<Podcast> findByFeedUrl(String feedUrl) {
        return podcasts.findByFeedUrl(feedUrl).map(PodcastEntity::toDomain);
    }

    @Override
    public List<Episode> episodes(UUID podcastId) {
        return episodes.findByPodcastIdOrderByPublishedAtDesc(podcastId).stream().map(EpisodeEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public Podcast saveSynced(Podcast current, String feedUrl, PodcastDraft draft, FeedSnapshot snapshot, int maxEpisodes) {
        PodcastEntity entity = current == null
                ? PodcastEntity.create(UUID.randomUUID(), feedUrl)
                : podcasts.findById(current.id()).orElseGet(() -> PodcastEntity.create(current.id(), feedUrl));
        entity.apply(
                PlainText.clip(PlainText.first(snapshot.title(), draft.title(), "Podcast"), 240),
                PlainText.clip(PlainText.first(snapshot.author(), draft.author()), 160),
                PlainText.clip(snapshot.description(), 1200),
                PlainText.clip(PlainText.first(snapshot.artworkUrl(), draft.artworkUrl()), 1000),
                Instant.now());
        PodcastEntity saved = podcasts.save(entity);
        for (FeedEpisode episode : snapshot.episodes()) {
            EpisodeEntity stored = episodes.findByPodcastIdAndGuid(saved.getId(), episode.guid())
                    .orElseGet(() -> EpisodeEntity.create(saved.getId(), episode.guid()));
            stored.apply(
                    episode.title(),
                    episode.summary(),
                    episode.audioUrl(),
                    episode.artworkUrl() == null ? "" : episode.artworkUrl(),
                    episode.publishedAt(),
                    episode.durationSeconds());
            episodes.save(stored);
        }
        List<EpisodeEntity> all = episodes.findByPodcastIdOrderByPublishedAtDesc(saved.getId());
        if (all.size() > maxEpisodes) {
            episodes.deleteAll(List.copyOf(all.subList(maxEpisodes, all.size())));
        }
        return saved.toDomain();
    }

    @Override
    public List<Podcast> findStaleAmong(Collection<UUID> ids, Instant cutoff) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return podcasts.findStaleAmong(ids, cutoff).stream().map(PodcastEntity::toDomain).toList();
    }
}
