package com.gbreda.fila.feed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FeedParserTest {

    private final FeedParser parser = new FeedParser();

    @Test
    void readsPlayableEpisodesInReleaseOrder() throws Exception {
        byte[] body = Files.readAllBytes(Path.of("src/test/resources/sample-feed.xml"));
        ParsedFeed feed = parser.parse("https://example.com/feed.xml", body);

        assertThat(feed.title()).isEqualTo("Show Teste");
        assertThat(feed.author()).isEqualTo("Autora Teste");
        assertThat(feed.description()).isEqualTo("Um podcast de teste.");
        assertThat(feed.artworkUrl()).isEqualTo("https://cdn.example.com/show.jpg");
        assertThat(feed.episodes()).extracting(ParsedEpisode::title)
                .containsExactly("Episódio novo", "Episódio do meio", "Episódio antigo");

        ParsedEpisode newest = feed.episodes().getFirst();
        assertThat(newest.durationSeconds()).isEqualTo(3723);
        assertThat(newest.description()).isEqualTo("Olá mundo");
        assertThat(newest.artworkUrl()).isEqualTo("https://cdn.example.com/novo.jpg");
        assertThat(newest.audioUrl()).isEqualTo("https://cdn.example.com/novo.mp3");
        assertThat(feed.episodes().get(1).audioUrl()).isEqualTo("https://example.com/meio.mp3");
        assertThat(feed.episodes().get(1).durationSeconds()).isEqualTo(125);
        assertThat(feed.episodes().get(2).durationSeconds()).isEqualTo(2700);
    }

    @Test
    void parsesDurationShapes() {
        assertThat(FeedParser.parseDuration("01:02:03")).isEqualTo(3723);
        assertThat(FeedParser.parseDuration("45:00")).isEqualTo(2700);
        assertThat(FeedParser.parseDuration("125")).isEqualTo(125);
        assertThat(FeedParser.parseDuration("")).isNull();
        assertThat(FeedParser.parseDuration("nope")).isNull();
    }
}
