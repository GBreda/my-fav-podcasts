package com.gbreda.fila.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FeedRefreshScheduler {

    private final PodcastService podcasts;

    public FeedRefreshScheduler(PodcastService podcasts) {
        this.podcasts = podcasts;
    }

    @Scheduled(fixedDelay = 1_800_000, initialDelay = 120_000)
    public void refresh() {
        podcasts.refreshAll();
    }
}
