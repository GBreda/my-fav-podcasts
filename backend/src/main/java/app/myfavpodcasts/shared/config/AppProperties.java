package app.myfavpodcasts.shared.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Feed feed, List<String> corsOrigins) {

    public record Jwt(String secret, int ttlDays) {
    }

    public record Feed(int maxEpisodes, Duration minInterval) {
    }
}
