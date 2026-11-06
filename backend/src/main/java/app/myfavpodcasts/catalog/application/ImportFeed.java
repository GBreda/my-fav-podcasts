package app.myfavpodcasts.catalog.application;

import app.myfavpodcasts.catalog.domain.FeedReader;
import app.myfavpodcasts.catalog.domain.FeedSnapshot;
import app.myfavpodcasts.catalog.domain.Podcast;
import app.myfavpodcasts.catalog.domain.PodcastDraft;
import app.myfavpodcasts.catalog.domain.PodcastRepository;
import app.myfavpodcasts.shared.config.AppProperties;
import app.myfavpodcasts.shared.domain.SafeUrls;
import java.net.URI;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ImportFeed {

    private final FeedReader feeds;
    private final PodcastRepository podcasts;
    private final AppProperties properties;

    public ImportFeed(FeedReader feeds, PodcastRepository podcasts, AppProperties properties) {
        this.feeds = feeds;
        this.podcasts = podcasts;
        this.properties = properties;
    }

    public Podcast execute(PodcastDraft draft, boolean force) {
        String feedUrl = SafeUrls.normalize(draft.feedUrl());
        SafeUrls.requirePublic(feedUrl);
        Optional<Podcast> existing = podcasts.findByFeedUrl(feedUrl);
        if (!force && existing.isPresent() && existing.get().isFresh(properties.feed().minInterval())) {
            return existing.get();
        }
        FeedSnapshot snapshot = feeds.fetch(URI.create(feedUrl));
        int maxEpisodes = Math.max(1, properties.feed().maxEpisodes());
        try {
            return podcasts.saveSynced(existing.orElse(null), feedUrl, draft, snapshot, maxEpisodes);
        } catch (DataIntegrityViolationException exception) {
            return podcasts.findByFeedUrl(feedUrl).orElseThrow(() -> exception);
        }
    }
}
