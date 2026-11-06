package app.myfavpodcasts.listening.infrastructure;

import app.myfavpodcasts.catalog.application.SearchPodcasts;
import app.myfavpodcasts.catalog.domain.Episode;
import app.myfavpodcasts.catalog.domain.Podcast;
import app.myfavpodcasts.catalog.domain.PodcastDraft;
import app.myfavpodcasts.catalog.domain.PodcastHit;
import app.myfavpodcasts.catalog.domain.PodcastRepository;
import app.myfavpodcasts.listening.application.FollowPodcast;
import app.myfavpodcasts.listening.application.GetQueue;
import app.myfavpodcasts.listening.application.RecordProgress;
import app.myfavpodcasts.listening.application.RefreshResult;
import app.myfavpodcasts.listening.application.RefreshSubscriptions;
import app.myfavpodcasts.listening.application.UnfollowPodcast;
import app.myfavpodcasts.listening.domain.ListeningRepository;
import app.myfavpodcasts.listening.domain.Progress;
import app.myfavpodcasts.listening.domain.QueueFilter;
import app.myfavpodcasts.listening.domain.QueueItem;
import app.myfavpodcasts.listening.domain.ReleaseOrder;
import app.myfavpodcasts.listening.domain.SubscriptionView;
import app.myfavpodcasts.shared.domain.DomainException;
import app.myfavpodcasts.shared.security.CurrentUsers;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ListeningController {

    private final SearchPodcasts searchPodcasts;
    private final FollowPodcast followPodcast;
    private final UnfollowPodcast unfollowPodcast;
    private final GetQueue getQueue;
    private final RecordProgress recordProgress;
    private final RefreshSubscriptions refreshSubscriptions;
    private final ListeningRepository listening;
    private final PodcastRepository podcasts;

    public ListeningController(
            SearchPodcasts searchPodcasts,
            FollowPodcast followPodcast,
            UnfollowPodcast unfollowPodcast,
            GetQueue getQueue,
            RecordProgress recordProgress,
            RefreshSubscriptions refreshSubscriptions,
            ListeningRepository listening,
            PodcastRepository podcasts
    ) {
        this.searchPodcasts = searchPodcasts;
        this.followPodcast = followPodcast;
        this.unfollowPodcast = unfollowPodcast;
        this.getQueue = getQueue;
        this.recordProgress = recordProgress;
        this.refreshSubscriptions = refreshSubscriptions;
        this.listening = listening;
        this.podcasts = podcasts;
    }

    @GetMapping("/api/catalog/search")
    SearchResponse search(@RequestParam String q, @RequestParam(defaultValue = "BR") String country) {
        UUID userId = CurrentUsers.require().id();
        Map<String, SubscriptionView> followed = listening.list(userId).stream()
                .collect(Collectors.toMap(SubscriptionView::feedUrl, Function.identity(), (left, right) -> left));
        List<SearchHit> results = searchPodcasts.execute(q, country).stream()
                .map(hit -> SearchHit.from(hit, followed.get(hit.feedUrl())))
                .toList();
        return new SearchResponse(results);
    }

    @GetMapping("/api/catalog/podcasts/{id}")
    PodcastResponse podcast(@PathVariable UUID id) {
        UUID userId = CurrentUsers.require().id();
        Podcast podcast = podcasts.findById(id).orElseThrow(() -> new DomainException("PODCAST_NOT_FOUND"));
        boolean followed = listening.subscriptionExists(userId, id);
        List<EpisodeResponse> episodes = podcasts.episodes(id).stream().map(EpisodeResponse::from).toList();
        return PodcastResponse.from(podcast, followed, episodes);
    }

    @GetMapping("/api/subscriptions")
    SubscriptionListResponse subscriptions() {
        List<SubscriptionResponse> items = listening.list(CurrentUsers.require().id()).stream()
                .map(SubscriptionResponse::from)
                .toList();
        return new SubscriptionListResponse(items);
    }

    @PostMapping("/api/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    PodcastResponse follow(@Valid @RequestBody FollowBody body) {
        Podcast podcast = followPodcast.execute(CurrentUsers.require().id(), new PodcastDraft(
                body.feedUrl(),
                body.title(),
                body.author(),
                body.artworkUrl()));
        return PodcastResponse.from(podcast, true, List.of());
    }

    @DeleteMapping("/api/subscriptions/{podcastId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unfollow(@PathVariable UUID podcastId) {
        unfollowPodcast.execute(CurrentUsers.require().id(), podcastId);
    }

    @GetMapping("/api/queue")
    QueueResponse queue(
            @RequestParam(defaultValue = "unplayed") String filter,
            @RequestParam(defaultValue = "newest") String order
    ) {
        List<QueueEntry> entries = getQueue.execute(
                        CurrentUsers.require().id(),
                        QueueFilter.from(filter),
                        ReleaseOrder.from(order))
                .stream()
                .map(QueueEntry::from)
                .toList();
        return new QueueResponse(entries);
    }

    @PostMapping("/api/queue/refresh")
    RefreshResponse refresh() {
        RefreshResult result = refreshSubscriptions.execute(CurrentUsers.require().id());
        return new RefreshResponse(result.synced(), result.failed());
    }

    @PostMapping("/api/queue/episodes/{episodeId}/progress")
    ProgressResponse progress(@PathVariable UUID episodeId, @Valid @RequestBody ProgressBody body) {
        Progress progress = recordProgress.execute(
                CurrentUsers.require().id(),
                episodeId,
                body.positionSeconds(),
                body.completed());
        return new ProgressResponse(progress.positionSeconds(), progress.completed());
    }

    public record FollowBody(
            @NotBlank @Size(max = 1000) String feedUrl,
            @Size(max = 240) String title,
            @Size(max = 160) String author,
            @Size(max = 1000) String artworkUrl
    ) {
    }

    public record ProgressBody(@Min(0) @Max(1_000_000) int positionSeconds, boolean completed) {
    }

    public record SearchHit(
            String itunesId,
            String title,
            String author,
            String artworkUrl,
            String feedUrl,
            String genre,
            boolean followed,
            UUID podcastId
    ) {
        static SearchHit from(PodcastHit hit, SubscriptionView subscription) {
            return new SearchHit(
                    hit.itunesId(),
                    hit.title(),
                    hit.author(),
                    hit.artworkUrl(),
                    hit.feedUrl(),
                    hit.genre(),
                    subscription != null,
                    subscription == null ? null : subscription.id());
        }
    }

    public record SearchResponse(List<SearchHit> results) {
    }

    public record EpisodeResponse(
            UUID id,
            String title,
            String summary,
            String audioUrl,
            String artworkUrl,
            @JsonFormat(shape = JsonFormat.Shape.STRING) Instant publishedAt,
            Integer durationSeconds
    ) {
        static EpisodeResponse from(Episode episode) {
            return new EpisodeResponse(
                    episode.id(),
                    episode.title(),
                    episode.summary(),
                    episode.audioUrl(),
                    episode.artworkUrl(),
                    episode.publishedAt(),
                    episode.durationSeconds());
        }
    }

    public record PodcastResponse(
            UUID id,
            String title,
            String author,
            String description,
            String artworkUrl,
            String feedUrl,
            boolean followed,
            List<EpisodeResponse> episodes
    ) {
        static PodcastResponse from(Podcast podcast, boolean followed, List<EpisodeResponse> episodes) {
            return new PodcastResponse(
                    podcast.id(),
                    podcast.title(),
                    podcast.author(),
                    podcast.description(),
                    podcast.artworkUrl(),
                    podcast.feedUrl(),
                    followed,
                    episodes);
        }
    }

    public record SubscriptionResponse(
            UUID id,
            String title,
            String author,
            String artworkUrl,
            String feedUrl
    ) {
        static SubscriptionResponse from(SubscriptionView view) {
            return new SubscriptionResponse(view.id(), view.title(), view.author(), view.artworkUrl(), view.feedUrl());
        }
    }

    public record SubscriptionListResponse(List<SubscriptionResponse> items) {
    }

    public record QueueEntry(
            UUID episodeId,
            UUID podcastId,
            String podcastTitle,
            String title,
            String summary,
            String audioUrl,
            String artworkUrl,
            @JsonFormat(shape = JsonFormat.Shape.STRING) Instant publishedAt,
            Integer durationSeconds,
            int positionSeconds,
            boolean completed
    ) {
        static QueueEntry from(QueueItem item) {
            return new QueueEntry(
                    item.episodeId(),
                    item.podcastId(),
                    item.podcastTitle(),
                    item.title(),
                    item.summary(),
                    item.audioUrl(),
                    item.artworkUrl(),
                    item.publishedAt(),
                    item.durationSeconds(),
                    item.positionSeconds(),
                    item.completed());
        }
    }

    public record QueueResponse(List<QueueEntry> entries) {
    }

    public record RefreshResponse(int synced, int failed) {
    }

    public record ProgressResponse(int positionSeconds, boolean completed) {
    }
}
