package app.myfavpodcasts.listening.application;

import app.myfavpodcasts.catalog.application.ImportFeed;
import app.myfavpodcasts.catalog.domain.Podcast;
import app.myfavpodcasts.catalog.domain.PodcastDraft;
import app.myfavpodcasts.listening.domain.ListeningRepository;
import app.myfavpodcasts.shared.domain.DomainException;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class FollowPodcast {

    private final ImportFeed imports;
    private final ListeningRepository listening;

    public FollowPodcast(ImportFeed imports, ListeningRepository listening) {
        this.imports = imports;
        this.listening = listening;
    }

    public Podcast execute(UUID userId, PodcastDraft draft) {
        Podcast podcast = imports.execute(draft, false);
        if (listening.subscriptionExists(userId, podcast.id())) {
            throw new DomainException("ALREADY_FOLLOWING");
        }
        try {
            listening.follow(userId, podcast.id());
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException("ALREADY_FOLLOWING");
        }
        return podcast;
    }
}
