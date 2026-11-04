package com.gbreda.fila.service;

import com.gbreda.fila.domain.Episode;
import com.gbreda.fila.repo.EpisodeRepository;
import com.gbreda.fila.web.ApiException;
import com.gbreda.fila.web.NotFoundException;
import com.gbreda.fila.web.dto.ApiModels.EpisodeResponse;
import com.gbreda.fila.web.dto.ApiModels.EpisodeUpdate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EpisodeService {

    private final EpisodeRepository episodes;
    private final EpisodeMapper mapper;

    public EpisodeService(EpisodeRepository episodes, EpisodeMapper mapper) {
        this.episodes = episodes;
        this.mapper = mapper;
    }

    @Transactional
    public EpisodeResponse update(UUID id, EpisodeUpdate update) {
        if (update == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nada para atualizar.");
        }
        Episode episode = episodes.findWithPodcast(id)
                .orElseThrow(() -> new NotFoundException("Episódio não encontrado."));
        if (update.positionSeconds() != null) {
            if (update.positionSeconds() < 0 || update.positionSeconds() > 86_400) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Posição inválida.");
            }
            episode.setPositionSeconds(update.positionSeconds());
        }
        if (update.played() != null) {
            episode.applyPlayed(update.played());
        }
        return mapper.toResponse(episode);
    }
}
