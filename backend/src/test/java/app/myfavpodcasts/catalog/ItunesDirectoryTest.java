package app.myfavpodcasts.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import app.myfavpodcasts.catalog.domain.PodcastHit;
import app.myfavpodcasts.catalog.infrastructure.ItunesDirectory;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ItunesDirectoryTest {

    @Test
    void keepsPodcastsThatPublishAFeed() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] body = """
                {"resultCount":3,"results":[
                  {"kind":"podcast","collectionId":10,"collectionName":"Nerdcast","artistName":"Jovem Nerd","feedUrl":"https://example.com/nerd/","artworkUrl600":"https://example.com/a.jpg","primaryGenreName":"Comedy"},
                  {"kind":"podcast","collectionId":12,"collectionName":"Nerdcast de novo","artistName":"Jovem Nerd","feedUrl":"https://example.com/nerd"},
                  {"kind":"podcast","collectionId":11,"collectionName":"Sem feed","artistName":"X"}
                ]}
                """.getBytes(StandardCharsets.UTF_8);
        server.createContext("/search", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            ItunesDirectory directory = new ItunesDirectory(HttpClient.newHttpClient(), base, JsonMapper.builder().build());

            List<PodcastHit> hits = directory.search("nerd", "BR");

            assertThat(hits).hasSize(1);
            assertThat(hits.get(0).title()).isEqualTo("Nerdcast");
            assertThat(hits.get(0).feedUrl()).isEqualTo("https://example.com/nerd");
            assertThat(hits.get(0).itunesId()).isEqualTo("10");
            assertThat(directory.search("a", "BR")).isEmpty();
        } finally {
            server.stop(0);
        }
    }
}
