package app.myfavpodcasts.catalog.infrastructure;

import app.myfavpodcasts.catalog.domain.PodcastDirectory;
import app.myfavpodcasts.catalog.domain.PodcastHit;
import app.myfavpodcasts.shared.domain.DomainException;
import app.myfavpodcasts.shared.domain.SafeUrls;
import app.myfavpodcasts.shared.text.PlainText;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ItunesDirectory implements PodcastDirectory {

    private final HttpClient http;
    private final URI base;
    private final JsonMapper json;

    @Autowired
    public ItunesDirectory(HttpClient http, JsonMapper json) {
        this(http, URI.create("https://itunes.apple.com"), json);
    }

    public ItunesDirectory(HttpClient http, URI base, JsonMapper json) {
        this.http = http;
        this.base = base;
        this.json = json;
    }

    @Override
    public List<PodcastHit> search(String query, String country) {
        if (query == null || query.length() < 2) {
            return List.of();
        }
        String term = query.length() > 80 ? query.substring(0, 80) : query;
        URI uri = URI.create(base + "/search?term=" + URLEncoder.encode(term, StandardCharsets.UTF_8)
                + "&media=podcast&entity=podcast&limit=20&country=" + country);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "Fila/1.0")
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new DomainException("DIRECTORY_UNAVAILABLE");
            }
            return parse(response.body());
        } catch (IOException exception) {
            throw new DomainException("DIRECTORY_UNAVAILABLE");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DomainException("DIRECTORY_UNAVAILABLE");
        }
    }

    private List<PodcastHit> parse(String body) {
        JsonNode results = json.readTree(body).path("results");
        if (!results.isArray()) {
            return List.of();
        }
        Map<String, PodcastHit> hits = new LinkedHashMap<>();
        for (JsonNode node : results) {
            if (!"podcast".equals(node.path("kind").asString(""))) {
                continue;
            }
            String feed = node.path("feedUrl").asString("");
            if (feed.isBlank()) {
                continue;
            }
            String normalized;
            try {
                normalized = SafeUrls.normalize(feed);
            } catch (DomainException exception) {
                continue;
            }
            String artwork = PlainText.first(
                    node.path("artworkUrl600").asString(""),
                    node.path("artworkUrl100").asString(""));
            hits.putIfAbsent(normalized, new PodcastHit(
                    node.path("collectionId").asString(""),
                    PlainText.clip(node.path("collectionName").asString("Podcast"), 240),
                    PlainText.clip(node.path("artistName").asString(""), 160),
                    artwork,
                    normalized,
                    PlainText.clip(node.path("primaryGenreName").asString(""), 80)));
        }
        return new ArrayList<>(hits.values());
    }
}
