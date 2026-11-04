package com.gbreda.fila.web;

import com.gbreda.fila.service.EpisodeService;
import com.gbreda.fila.web.dto.ApiModels.EpisodeResponse;
import com.gbreda.fila.web.dto.ApiModels.EpisodeUpdate;
import java.util.UUID;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/episodes")
public class EpisodeController {

    private final EpisodeService episodes;

    public EpisodeController(EpisodeService episodes) {
        this.episodes = episodes;
    }

    @PatchMapping("/{id}")
    public EpisodeResponse update(@PathVariable UUID id, @RequestBody EpisodeUpdate update) {
        return episodes.update(id, update);
    }
}
