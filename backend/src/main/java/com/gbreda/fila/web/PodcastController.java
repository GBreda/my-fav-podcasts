package com.gbreda.fila.web;

import com.gbreda.fila.service.PodcastService;
import com.gbreda.fila.service.PodcastService.SubscribeOutcome;
import com.gbreda.fila.web.dto.ApiModels.PodcastDetail;
import com.gbreda.fila.web.dto.ApiModels.PodcastSummary;
import com.gbreda.fila.web.dto.ApiModels.RefreshResponse;
import com.gbreda.fila.web.dto.ApiModels.SearchResponse;
import com.gbreda.fila.web.dto.ApiModels.SubscribeRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/podcasts")
public class PodcastController {

    private final PodcastService podcasts;

    public PodcastController(PodcastService podcasts) {
        this.podcasts = podcasts;
    }

    @GetMapping
    public List<PodcastSummary> list() {
        return podcasts.list();
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam("q") String query) {
        return podcasts.search(query);
    }

    @PostMapping
    public ResponseEntity<PodcastSummary> subscribe(@RequestBody SubscribeRequest request) {
        SubscribeOutcome outcome = podcasts.subscribe(request);
        HttpStatus status = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(outcome.summary());
    }

    @PostMapping("/refresh")
    public RefreshResponse refreshAll() {
        return podcasts.refreshAll();
    }

    @GetMapping("/{id}")
    public PodcastDetail get(@PathVariable UUID id) {
        return podcasts.get(id);
    }

    @PostMapping("/{id}/refresh")
    public PodcastDetail refresh(@PathVariable UUID id) {
        return podcasts.refresh(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        podcasts.delete(id);
    }
}
