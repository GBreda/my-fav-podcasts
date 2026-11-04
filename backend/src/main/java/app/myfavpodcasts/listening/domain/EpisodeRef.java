package app.myfavpodcasts.listening.domain;

import java.util.UUID;

public record EpisodeRef(UUID id, Integer durationSeconds) {
}
