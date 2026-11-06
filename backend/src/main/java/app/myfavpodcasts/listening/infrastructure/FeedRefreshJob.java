package app.myfavpodcasts.listening.infrastructure;

import app.myfavpodcasts.catalog.application.ImportFeed;
import app.myfavpodcasts.catalog.domain.Podcast;
import app.myfavpodcasts.catalog.domain.PodcastDraft;
import app.myfavpodcasts.catalog.domain.PodcastRepository;
import app.myfavpodcasts.listening.domain.ListeningRepository;
import app.myfavpodcasts.shared.config.AppProperties;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class FeedRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(FeedRefreshJob.class);

    private final ListeningRepository listening;
    private final PodcastRepository podcasts;
    private final ImportFeed imports;
    private final AppProperties properties;

    public FeedRefreshJob(
            ListeningRepository listening,
            PodcastRepository podcasts,
            ImportFeed imports,
            AppProperties properties
    ) {
        this.listening = listening;
        this.podcasts = podcasts;
        this.imports = imports;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.feed.min-interval:15m}")
    public void refreshStale() {
        Instant cutoff = Instant.now().minus(properties.feed().minInterval());
        for (Podcast podcast : podcasts.findStaleAmong(listening.subscribedPodcastIds(), cutoff)) {
            try {
                imports.execute(new PodcastDraft(podcast.feedUrl(), podcast.title(), podcast.author(), podcast.artworkUrl()), true);
            } catch (RuntimeException exception) {
                log.warn("refresh failed for {}: {}", podcast.feedUrl(), exception.toString());
            }
        }
    }
}
