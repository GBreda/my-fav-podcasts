package com.gbreda.fila.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gbreda.fila.web.InvalidFeedException;
import java.net.URI;
import org.junit.jupiter.api.Test;

class FeedUrlValidatorTest {

    private final FeedUrlValidator urls = new FeedUrlValidator();

    @Test
    void normalizesHostAndDefaultPort() {
        URI uri = urls.parse("https://EXAMPLE.com:443/a/");
        assertThat(urls.normalize(uri)).isEqualTo("https://example.com/a");
    }

    @Test
    void rejectsNonPublicTargets() {
        assertThatThrownBy(() -> urls.parse("file:///etc/passwd")).isInstanceOf(InvalidFeedException.class);
        assertThatThrownBy(() -> urls.parse("http://127.0.0.1/feed")).isInstanceOf(InvalidFeedException.class);
        assertThatThrownBy(() -> urls.parse("http://10.1.2.3/feed")).isInstanceOf(InvalidFeedException.class);
        assertThatThrownBy(() -> urls.parse("http://192.168.0.8/feed")).isInstanceOf(InvalidFeedException.class);
        assertThatThrownBy(() -> urls.parse("http://169.254.169.254/latest")).isInstanceOf(InvalidFeedException.class);
        assertThatThrownBy(() -> urls.parse("http://localhost/feed")).isInstanceOf(InvalidFeedException.class);
        assertThatThrownBy(() -> urls.parse("http://user:pass@example.com/feed")).isInstanceOf(InvalidFeedException.class);
        assertThatThrownBy(() -> urls.parse("   ")).isInstanceOf(InvalidFeedException.class);
    }
}
