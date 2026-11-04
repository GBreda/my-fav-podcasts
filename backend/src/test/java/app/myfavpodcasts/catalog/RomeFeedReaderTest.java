package app.myfavpodcasts.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import app.myfavpodcasts.catalog.domain.FeedEpisode;
import app.myfavpodcasts.catalog.domain.FeedSnapshot;
import app.myfavpodcasts.catalog.infrastructure.RomeFeedReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RomeFeedReaderTest {

    @Test
    void readsAudioEpisodesFromRss() throws Exception {
        byte[] xml = Files.readAllBytes(Path.of("src/test/resources/fixtures/sample-feed.xml"));

        FeedSnapshot snapshot = RomeFeedReader.parse(xml);

        assertThat(snapshot.title()).isEqualTo("Canal de Teste");
        assertThat(snapshot.author()).isEqualTo("Equipe");
        assertThat(snapshot.description()).isEqualTo("Um podcast & mais.");
        assertThat(snapshot.artworkUrl()).isEqualTo("https://cdn.example.com/show.jpg");
        assertThat(snapshot.episodes()).extracting(FeedEpisode::title)
                .containsExactly("Episodio novo", "Episodio antigo");
        assertThat(snapshot.episodes().get(0).durationSeconds()).isEqualTo(3723);
        assertThat(snapshot.episodes().get(0).summary()).isEqualTo("Ola mundo");
        assertThat(snapshot.episodes().get(1).durationSeconds()).isEqualTo(90);
        assertThat(snapshot.episodes().get(1).audioUrl()).isEqualTo("https://cdn.example.com/old.m4a");
    }

    @Test
    void stopsAfterTheNewestFortyEpisodes() throws Exception {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0"><channel><title>Longo</title><description>d</description>
                """);
        for (int i = 0; i < 55; i++) {
            xml.append("""
                    <item><title>Ep %d</title><guid>g-%d</guid>
                    <pubDate>Wed, 04 Nov 2026 12:%02d:00 GMT</pubDate>
                    <enclosure url="https://cdn.example.com/%d.mp3" type="audio/mpeg" length="1"/>
                    </item>
                    """.formatted(i, i, i % 60, i));
        }
        xml.append("<description>");
        xml.append("x".repeat(3_000_000));
        xml.append("</description></channel></rss>");

        FeedSnapshot snapshot = RomeFeedReader.parse(RomeFeedReader.limit(new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8))));

        assertThat(snapshot.title()).isEqualTo("Longo");
        assertThat(snapshot.episodes()).hasSize(40);
        assertThat(snapshot.episodes()).extracting(FeedEpisode::title).contains("Ep 0", "Ep 39").doesNotContain("Ep 40");
    }
}
