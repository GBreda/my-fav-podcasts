package app.myfavpodcasts.listening;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.myfavpodcasts.catalog.domain.FeedEpisode;
import app.myfavpodcasts.catalog.domain.FeedReader;
import app.myfavpodcasts.catalog.domain.FeedSnapshot;
import app.myfavpodcasts.catalog.domain.PodcastDirectory;
import app.myfavpodcasts.catalog.domain.PodcastHit;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ListeningFlowTest {

    private static final String SHOW_A = "https://93.184.216.34/show-a.xml";
    private static final String SHOW_B = "https://93.184.216.34/show-b.xml";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedReader feedReader;

    @MockitoBean
    private PodcastDirectory directory;

    @BeforeEach
    void feeds() {
        when(feedReader.fetch(any())).thenAnswer(invocation -> {
            URI uri = invocation.getArgument(0);
            if (uri.toString().contains("show-b")) {
                return snapshot("Show B", "B novo", "2026-11-04T10:00:00Z", "B antigo", "2026-11-01T10:00:00Z");
            }
            return snapshot("Show A", "A novo", "2026-11-03T10:00:00Z", "A antigo", "2026-11-02T10:00:00Z");
        });
        when(directory.search("nerd", "BR")).thenReturn(List.of(
                new PodcastHit("10", "Show A", "Ana", "https://cdn.example.com/a.jpg", SHOW_A, "Society")));
    }

    @Test
    void registersFollowsAndBuildsOneQueueByReleaseDate() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"curta","displayName":"Ana"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));

        String token = tokenOf(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"senha-forte","displayName":"Ana"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.displayName").value("Ana"))
                .andReturn());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"senha-forte","displayName":"Ana"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"erradaaaa"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feedUrl":"http://127.0.0.1/secret.xml"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FEED_URL_BLOCKED"));

        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(follow(SHOW_A)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Show A"));

        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(follow(SHOW_B)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(follow(SHOW_A)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/queue")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].title").value("B novo"))
                .andExpect(jsonPath("$.entries[1].title").value("A novo"))
                .andExpect(jsonPath("$.entries[2].title").value("A antigo"))
                .andExpect(jsonPath("$.entries[3].title").value("B antigo"))
                .andExpect(jsonPath("$.entries[0].publishedAt").value("2026-11-04T10:00:00Z"));

        mockMvc.perform(get("/api/queue").param("order", "oldest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.entries[0].title").value("B antigo"));

        mockMvc.perform(get("/api/catalog/search").param("q", "nerd")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].followed").value(true));

        String episodeId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/queue").header("Authorization", "Bearer " + token))
                        .andReturn().getResponse().getContentAsString(),
                "$.entries[0].episodeId");

        mockMvc.perform(post("/api/queue/episodes/" + episodeId + "/progress")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"positionSeconds":12,"completed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(get("/api/queue").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.entries[0].title").value("A novo"))
                .andExpect(jsonPath("$.entries.length()").value(3));

        mockMvc.perform(get("/api/queue").param("filter", "all").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.entries.length()").value(4));

        mockMvc.perform(post("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"senha-forte","nextPassword":"outra-senha"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"outra-senha"}
                                """))
                .andExpect(status().isOk());

        String podcastId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/subscriptions").header("Authorization", "Bearer " + token))
                        .andReturn().getResponse().getContentAsString(),
                "$.items[0].id");
        mockMvc.perform(delete("/api/subscriptions/" + podcastId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    private static String tokenOf(MvcResult result) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private static String follow(String feedUrl) {
        return "{\"feedUrl\":\"" + feedUrl + "\",\"title\":\"hint\",\"author\":\"Ana\",\"artworkUrl\":\"https://cdn.example.com/a.jpg\"}";
    }

    private static FeedSnapshot snapshot(String show, String newer, String newerAt, String older, String olderAt) {
        return new FeedSnapshot(show, "Ana", "descricao", "https://cdn.example.com/a.jpg", List.of(
                new FeedEpisode(newer, newer, "resumo", "https://cdn.example.com/" + newer + ".mp3", "", Instant.parse(newerAt), 100),
                new FeedEpisode(older, older, "resumo", "https://cdn.example.com/" + older + ".mp3", "", Instant.parse(olderAt), 80)
        ));
    }
}
