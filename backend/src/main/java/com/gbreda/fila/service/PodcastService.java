package com.gbreda.fila.service;

import com.gbreda.fila.domain.Episode;
import com.gbreda.fila.domain.Podcast;
import com.gbreda.fila.feed.FeedFetcher;
import com.gbreda.fila.feed.FeedParser;
import com.gbreda.fila.feed.FeedUrlValidator;
import com.gbreda.fila.feed.ParsedEpisode;
import com.gbreda.fila.feed.ParsedFeed;
import com.gbreda.fila.itunes.ItunesClient;
import com.gbreda.fila.repo.EpisodeRepository;
import com.gbreda.fila.repo.PodcastRepository;
import com.gbreda.fila.web.ApiException;
import com.gbreda.fila.web.InvalidFeedException;
import com.gbreda.fila.web.NotFoundException;
import com.gbreda.fila.web.dto.ApiModels.EpisodeResponse;
import com.gbreda.fila.web.dto.ApiModels.PodcastDetail;
import com.gbreda.fila.web.dto.ApiModels.PodcastSummary;
import com.gbreda.fila.web.dto.ApiModels.RefreshResponse;
import com.gbreda.fila.web.dto.ApiModels.SearchResponse;
import com.gbreda.fila.web.dto.ApiModels.SubscribeRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PodcastService {

    private static final Logger log = LoggerFactory.getLogger(PodcastService.class);

    private final PodcastRepository podcasts;
    private final EpisodeRepository episodes;
    private final FeedFetcher fetcher;
    private final FeedParser parser;
    private final FeedUrlValidator urls;
    private final ItunesClient itunes;
    private final EpisodeMapper mapper;
    private final PodcastService self;
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public PodcastService(
            PodcastRepository podcasts,
            EpisodeRepository episodes,
            FeedFetcher fetcher,
            FeedParser parser,
            FeedUrlValidator urls,
            ItunesClient itunes,
            EpisodeMapper mapper,
            @Lazy PodcastService self) {
        this.podcasts = podcasts;
        this.episodes = episodes;
        this.fetcher = fetcher;
        this.parser = parser;
        this.urls = urls;
        this.itunes = itunes;
        this.mapper = mapper;
        this.self = self;
    }

    public SubscribeOutcome subscribe(SubscribeRequest request) {
        Resolved resolved = resolve(request);
        synchronized (locks.computeIfAbsent(resolved.feedUrl(), key -> new Object())) {
            Optional<Podcast> existing = podcasts.findByFeedUrl(resolved.feedUrl());
            if (existing.isPresent()) {
                return new SubscribeOutcome(false, toSummary(existing.get()));
            }
            ParsedFeed parsed = load(resolved.uri());
            return new SubscribeOutcome(true, self.saveNew(resolved.feedUrl(), resolved.itunesId(), parsed));
        }
    }

    public SearchResponse search(String query) {
        String term = query == null ? "" : query.trim();
        if (term.length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Digite pelo menos 2 letras.");
        }
        if (term.length() > 80) {
            term = term.substring(0, 80);
        }
        Map<String, Podcast> byFeed = podcasts.findAll().stream()
                .collect(Collectors.toMap(Podcast::getFeedUrl, podcast -> podcast, (left, right) -> left));
        List<SearchResponse.Hit> results = new ArrayList<>();
        for (ItunesClient.Hit hit : itunes.search(term)) {
            String feedUrl = null;
            UUID podcastId = null;
            boolean subscribed = false;
            if (hit.feedUrl() != null) {
                try {
                    feedUrl = urls.normalize(urls.parse(hit.feedUrl()));
                } catch (InvalidFeedException exception) {
                    continue;
                }
                Podcast existing = byFeed.get(feedUrl);
                if (existing != null) {
                    subscribed = true;
                    podcastId = existing.getId();
                }
            }
            results.add(new SearchResponse.Hit(
                    hit.itunesId(),
                    hit.title(),
                    hit.author() == null ? "" : hit.author(),
                    hit.artworkUrl(),
                    feedUrl,
                    hit.genre() == null ? "" : hit.genre(),
                    subscribed,
                    podcastId));
        }
        return new SearchResponse(results);
    }

    @Transactional(readOnly = true)
    public List<PodcastSummary> list() {
        return podcasts.findAllByOrderByTitleAsc().stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public PodcastDetail get(UUID id) {
        Podcast podcast = podcasts.findById(id).orElseThrow(() -> new NotFoundException("Podcast não encontrado."));
        List<EpisodeResponse> items = episodes.findByPodcastIdOrderByPublishedAtDesc(id).stream()
                .map(mapper::toResponse)
                .toList();
        return new PodcastDetail(toSummary(podcast), items);
    }

    public PodcastDetail refresh(UUID id) {
        Podcast podcast = podcasts.findById(id).orElseThrow(() -> new NotFoundException("Podcast não encontrado."));
        URI uri = urls.parse(podcast.getFeedUrl());
        return self.applyRefresh(id, load(uri));
    }

    public RefreshResponse refreshAll() {
        int updated = 0;
        int failed = 0;
        for (UUID id : podcasts.findAll().stream().map(Podcast::getId).toList()) {
            try {
                refresh(id);
                updated++;
            } catch (RuntimeException exception) {
                failed++;
                log.warn("Falha ao atualizar {}: {}", id, exception.getMessage());
            }
        }
        return new RefreshResponse(updated, failed);
    }

    @Transactional
    public void delete(UUID id) {
        if (!podcasts.existsById(id)) {
            throw new NotFoundException("Podcast não encontrado.");
        }
        podcasts.deleteById(id);
    }

    @Transactional
    public PodcastSummary saveNew(String feedUrl, Long itunesId, ParsedFeed parsed) {
        Podcast podcast = new Podcast();
        podcast.setFeedUrl(feedUrl);
        podcast.setItunesId(itunesId);
        applyMetadata(podcast, parsed);
        podcasts.saveAndFlush(podcast);
        for (ParsedEpisode item : parsed.episodes()) {
            episodes.saveAndFlush(newEpisode(podcast, item));
        }
        return toSummary(podcast);
    }

    @Transactional
    public PodcastDetail applyRefresh(UUID id, ParsedFeed parsed) {
        Podcast podcast = podcasts.findById(id).orElseThrow(() -> new NotFoundException("Podcast não encontrado."));
        applyMetadata(podcast, parsed);
        for (ParsedEpisode item : parsed.episodes()) {
            Episode episode = episodes.findByPodcastIdAndGuid(id, item.guid()).orElse(null);
            if (episode == null) {
                episodes.save(newEpisode(podcast, item));
            } else {
                copyMutable(episode, item);
            }
        }
        return detail(podcast);
    }

    private PodcastDetail detail(Podcast podcast) {
        List<EpisodeResponse> items = episodes.findByPodcastIdOrderByPublishedAtDesc(podcast.getId()).stream()
                .map(mapper::toResponse)
                .toList();
        return new PodcastDetail(toSummary(podcast), items);
    }

    private ParsedFeed load(URI uri) {
        return parser.parse(urls.normalize(uri), fetcher.fetch(uri));
    }

    private Resolved resolve(SubscribeRequest request) {
        if (request == null) {
            throw new InvalidFeedException("Informe um podcast para seguir.");
        }
        String raw;
        Long itunesId = request.itunesId();
        if (itunesId != null) {
            ItunesClient.Hit hit = itunes.lookup(itunesId)
                    .orElseThrow(() -> new InvalidFeedException("Não encontrei esse podcast."));
            if (hit.feedUrl() == null || hit.feedUrl().isBlank()) {
                throw new InvalidFeedException("Esse podcast não publica um feed.");
            }
            raw = hit.feedUrl();
        } else if (request.feedUrl() != null && !request.feedUrl().isBlank()) {
            raw = request.feedUrl();
        } else {
            throw new InvalidFeedException("Informe um podcast para seguir.");
        }
        URI uri = urls.parse(raw);
        return new Resolved(uri, urls.normalize(uri), itunesId);
    }

    private PodcastSummary toSummary(Podcast podcast) {
        return new PodcastSummary(
                podcast.getId(),
                podcast.getTitle(),
                podcast.getAuthor() == null ? "" : podcast.getAuthor(),
                podcast.getDescription() == null ? "" : podcast.getDescription(),
                podcast.getArtworkUrl(),
                podcast.getFeedUrl(),
                (int) episodes.countByPodcastId(podcast.getId()),
                (int) episodes.countByPodcastIdAndPlayedFalse(podcast.getId()),
                podcast.getLastRefreshedAt());
    }

    private static void applyMetadata(Podcast podcast, ParsedFeed parsed) {
        podcast.setTitle(parsed.title());
        podcast.setAuthor(parsed.author() == null ? "" : parsed.author());
        podcast.setDescription(parsed.description() == null ? "" : parsed.description());
        podcast.setArtworkUrl(blankToNull(parsed.artworkUrl()));
        podcast.setLastRefreshedAt(java.time.Instant.now());
    }

    private static Episode newEpisode(Podcast podcast, ParsedEpisode item) {
        Episode episode = new Episode();
        episode.setPodcast(podcast);
        episode.setGuid(item.guid());
        copyMutable(episode, item);
        return episode;
    }

    private static void copyMutable(Episode episode, ParsedEpisode item) {
        episode.setTitle(item.title());
        episode.setDescription(item.description() == null ? "" : item.description());
        episode.setAudioUrl(item.audioUrl());
        episode.setArtworkUrl(blankToNull(item.artworkUrl()));
        episode.setPublishedAt(item.publishedAt());
        episode.setDurationSeconds(item.durationSeconds());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record SubscribeOutcome(boolean created, PodcastSummary summary) {}

    private record Resolved(URI uri, String feedUrl, Long itunesId) {}
}
