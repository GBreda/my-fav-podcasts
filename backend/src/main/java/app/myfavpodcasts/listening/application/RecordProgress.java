package app.myfavpodcasts.listening.application;

import app.myfavpodcasts.listening.domain.EpisodeRef;
import app.myfavpodcasts.listening.domain.ListeningRepository;
import app.myfavpodcasts.listening.domain.Progress;
import app.myfavpodcasts.shared.domain.DomainException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RecordProgress {

    private final ListeningRepository listening;

    public RecordProgress(ListeningRepository listening) {
        this.listening = listening;
    }

    public Progress execute(UUID userId, UUID episodeId, int positionSeconds, boolean completed) {
        EpisodeRef episode = listening.episodeForUser(userId, episodeId)
                .orElseThrow(() -> new DomainException("EPISODE_NOT_FOUND"));
        int position = Math.max(0, positionSeconds);
        boolean done = completed;
        if (episode.durationSeconds() != null && episode.durationSeconds() > 0 && position >= episode.durationSeconds() - 10) {
            done = true;
        }
        listening.saveProgress(userId, episodeId, position, done);
        return new Progress(position, done);
    }
}
