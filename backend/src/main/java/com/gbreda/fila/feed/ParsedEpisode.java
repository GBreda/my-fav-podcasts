package com.gbreda.fila.feed;

import java.time.Instant;

public record ParsedEpisode(
        String guid,
        String title,
        String description,
        String audioUrl,
        String artworkUrl,
        Instant publishedAt,
        Integer durationSeconds) {}
