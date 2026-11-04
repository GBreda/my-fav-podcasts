package app.myfavpodcasts.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.myfavpodcasts.shared.domain.DomainException;
import app.myfavpodcasts.shared.domain.SafeUrls;
import org.junit.jupiter.api.Test;

class SafeUrlsTest {

    @Test
    void normalizesPublicFeedUrls() {
        assertThat(SafeUrls.normalize("HTTPS://Example.COM/feed/"))
                .isEqualTo("https://example.com/feed");
    }

    @Test
    void blocksPrivateAndLocalTargets() {
        assertThatThrownBy(() -> SafeUrls.normalize("http://127.0.0.1/feed.xml"))
                .isInstanceOf(DomainException.class)
                .hasMessage("FEED_URL_BLOCKED");
        assertThatThrownBy(() -> SafeUrls.normalize("http://10.1.2.3/feed.xml"))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> SafeUrls.normalize("http://169.254.169.254/latest"))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> SafeUrls.normalize("file:///etc/passwd"))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> SafeUrls.normalize("http://localhost/feed.xml"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void acceptsAPublicIpLiteralWithoutDns() {
        SafeUrls.requirePublic(SafeUrls.normalize("https://93.184.216.34/show.xml"));
    }
}
