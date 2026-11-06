package app.myfavpodcasts.listening.application;

import app.myfavpodcasts.catalog.application.ImportFeed;
import app.myfavpodcasts.catalog.domain.PodcastDraft;
import app.myfavpodcasts.listening.domain.ListeningRepository;
import app.myfavpodcasts.listening.domain.SubscriptionView;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RefreshSubscriptions {

    private static final Logger log = LoggerFactory.getLogger(RefreshSubscriptions.class);

    private final ListeningRepository listening;
    private final ImportFeed imports;

    public RefreshSubscriptions(ListeningRepository listening, ImportFeed imports) {
        this.listening = listening;
        this.imports = imports;
    }

    public RefreshResult execute(UUID userId) {
        int synced = 0;
        int failed = 0;
        for (SubscriptionView subscription : listening.list(userId)) {
            try {
                imports.execute(new PodcastDraft(
                        subscription.feedUrl(),
                        subscription.title(),
                        subscription.author(),
                        subscription.artworkUrl()), true);
                synced++;
            } catch (RuntimeException exception) {
                failed++;
                log.warn("refresh failed for {}: {}", subscription.feedUrl(), exception.toString());
            }
        }
        return new RefreshResult(synced, failed);
    }
}
