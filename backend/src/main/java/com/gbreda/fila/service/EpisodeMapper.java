package com.gbreda.fila.service;

import com.gbreda.fila.domain.Episode;
import com.gbreda.fila.domain.Podcast;
import com.gbreda.fila.web.dto.ApiModels.EpisodeResponse;
import com.gbreda.fila.web.dto.ApiModels.ShowRef;
import org.springframework.stereotype.Component;

@Component
public class EpisodeMapper {

    public EpisodeResponse toResponse(Episode episode) {
        Podcast podcast = episode.getPodcast();
        return new EpisodeResponse(
                episode.getId(),
                episode.getTitle(),
                episode.getDescription() == null ? "" : episode.getDescription(),
                episode.getAudioUrl(),
                firstNonBlank(episode.getArtworkUrl(), podcast.getArtworkUrl()),
                episode.getPublishedAt(),
                episode.getDurationSeconds(),
                episode.isPlayed(),
                episode.getPositionSeconds(),
                new ShowRef(podcast.getId(), podcast.getTitle(), podcast.getArtworkUrl()));
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }
}
