package com.gbreda.fila.service;

import com.gbreda.fila.domain.Episode;
import com.gbreda.fila.repo.EpisodeRepository;
import com.gbreda.fila.web.dto.ApiModels.EpisodeResponse;
import com.gbreda.fila.web.dto.ApiModels.QueueResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueueService {

    private static final int PAGE = 300;

    private final EpisodeRepository episodes;
    private final EpisodeMapper mapper;

    public QueueService(EpisodeRepository episodes, EpisodeMapper mapper) {
        this.episodes = episodes;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public QueueResponse queue(String sort) {
        boolean oldest = "oldest".equalsIgnoreCase(sort);
        var page = PageRequest.of(0, PAGE);
        List<UUID> ids = oldest ? episodes.findUnplayedOldestIds(page) : episodes.findUnplayedNewestIds(page);
        long total = episodes.countByPlayedFalse();
        if (ids.isEmpty()) {
            return new QueueResponse(oldest ? "oldest" : "newest", total, List.of());
        }
        Map<UUID, Episode> byId = episodes.findAllWithPodcast(ids).stream()
                .collect(Collectors.toMap(Episode::getId, Function.identity()));
        List<EpisodeResponse> mapped = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(mapper::toResponse)
                .toList();
        return new QueueResponse(oldest ? "oldest" : "newest", total, mapped);
    }
}
