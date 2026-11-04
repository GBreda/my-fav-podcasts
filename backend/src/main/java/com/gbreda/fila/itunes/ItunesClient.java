package com.gbreda.fila.itunes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gbreda.fila.web.ApiException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ItunesClient {

    private final HttpClient http;
    private final ObjectMapper json;

    public ItunesClient(HttpClient http, ObjectMapper json) {
        this.http = http;
        this.json = json;
    }

    public List<Hit> search(String term) {
        String url = "https://itunes.apple.com/search?media=podcast&entity=podcast&limit=25&country=BR&term="
                + URLEncoder.encode(term, StandardCharsets.UTF_8);
        return readHits(get(url));
    }

    public Optional<Hit> lookup(long itunesId) {
        JsonNode root = get("https://itunes.apple.com/lookup?country=BR&id=" + itunesId);
        return readHits(root).stream().findFirst();
    }

    private List<Hit> readHits(JsonNode root) {
        List<Hit> hits = new ArrayList<>();
        for (JsonNode node : root.path("results")) {
            long id = node.path("collectionId").asLong(0);
            if (id == 0) {
                continue;
            }
            String title = text(node, "collectionName");
            if (title == null) {
                continue;
            }
            hits.add(new Hit(
                    id,
                    title,
                    text(node, "artistName"),
                    first(text(node, "artworkUrl600"), text(node, "artworkUrl100")),
                    text(node, "feedUrl"),
                    text(node, "primaryGenreName")));
        }
        return hits;
    }

    private JsonNode get(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "Fila/0.1 (podcast queue)")
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400 || response.body() == null) {
                throw unavailable();
            }
            return json.readTree(response.body());
        } catch (IOException exception) {
            throw unavailable();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        }
    }

    private static ApiException unavailable() {
        return new ApiException(HttpStatus.BAD_GATEWAY, "A busca de podcasts está indisponível agora.");
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private static String first(String preferred, String fallback) {
        return preferred != null ? preferred : fallback;
    }

    public record Hit(
            long itunesId,
            String title,
            String author,
            String artworkUrl,
            String feedUrl,
            String genre) {}
}
