package com.gbreda.fila.feed;

import java.util.List;

public record ParsedFeed(
        String title,
        String author,
        String description,
        String artworkUrl,
        List<ParsedEpisode> episodes) {}
