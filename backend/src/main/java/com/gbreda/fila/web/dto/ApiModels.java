package com.gbreda.fila.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApiModels {

    private ApiModels() {}

    public record PodcastSummary(
            UUID id,
            String title,
            String author,
            String description,
            String artworkUrl,
            String feedUrl,
            int episodeCount,
            int unplayedCount,
            Instant lastRefreshedAt) {}

    public record PodcastDetail(PodcastSummary podcast, List<EpisodeResponse> episodes) {}

    public record ShowRef(UUID id, String title, String artworkUrl) {}

    public record EpisodeResponse(
            UUID id,
            String title,
            String description,
            String audioUrl,
            String artworkUrl,
            Instant publishedAt,
            Integer durationSeconds,
            boolean played,
            int positionSeconds,
            ShowRef podcast) {}

    public record QueueResponse(String sort, long total, List<EpisodeResponse> episodes) {}

    public record SearchResponse(List<Hit> results) {

        public record Hit(
                long itunesId,
                String title,
                String author,
                String artworkUrl,
                String feedUrl,
                String genre,
                boolean subscribed,
                UUID podcastId) {}
    }

    public record SubscribeRequest(String feedUrl, Long itunesId) {}

    public record EpisodeUpdate(Integer positionSeconds, Boolean played) {}

    public record RefreshResponse(int updated, int failed) {}

    public record ApiError(String message) {}
}
