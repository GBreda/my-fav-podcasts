package app.myfavpodcasts.listening.application;

import app.myfavpodcasts.listening.domain.ListeningRepository;
import app.myfavpodcasts.shared.domain.DomainException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UnfollowPodcast {

    private final ListeningRepository listening;

    public UnfollowPodcast(ListeningRepository listening) {
        this.listening = listening;
    }

    public void execute(UUID userId, UUID podcastId) {
        if (!listening.unfollow(userId, podcastId)) {
            throw new DomainException("NOT_FOLLOWING");
        }
    }
}
