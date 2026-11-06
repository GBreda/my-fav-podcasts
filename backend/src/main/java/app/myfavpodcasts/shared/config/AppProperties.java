package app.myfavpodcasts.shared.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Feed feed, List<String> corsOrigins, String publicBaseUrl, String mailFrom) {

    public String publicBaseUrl() {
        String value = publicBaseUrl == null || publicBaseUrl.isBlank()
                ? "http://localhost:3000"
                : publicBaseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public String mailFrom() {
        return mailFrom == null || mailFrom.isBlank() ? "fila@localhost" : mailFrom.trim();
    }

    public record Jwt(String secret, int ttlDays) {
    }

    public record Feed(int maxEpisodes, Duration minInterval) {
    }
}
