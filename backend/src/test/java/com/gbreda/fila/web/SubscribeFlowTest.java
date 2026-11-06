package com.gbreda.fila.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gbreda.fila.feed.FeedFetcher;
import com.jayway.jsonpath.JsonPath;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SubscribeFlowTest.Stubs.class)
class SubscribeFlowTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void queueFollowsReleaseOrderAndKeepsPlayedAcrossRefresh() throws Exception {
        MvcResult created = mvc.perform(post("/api/podcasts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedUrl\":\"https://example.com/feed.xml\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Show Teste"))
                .andExpect(jsonPath("$.author").value("Autora Teste"))
                .andExpect(jsonPath("$.unplayedCount").value(3))
                .andReturn();
        String podcastId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sort").value("newest"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.episodes[0].title").value("Episódio novo"))
                .andExpect(jsonPath("$.episodes[0].durationSeconds").value(3723))
                .andExpect(jsonPath("$.episodes[1].title").value("Episódio do meio"))
                .andExpect(jsonPath("$.episodes[2].title").value("Episódio antigo"));

        mvc.perform(get("/api/queue").param("sort", "oldest"))
                .andExpect(jsonPath("$.sort").value("oldest"))
                .andExpect(jsonPath("$.episodes[0].title").value("Episódio antigo"))
                .andExpect(jsonPath("$.episodes[2].title").value("Episódio novo"));

        MvcResult queue = mvc.perform(get("/api/queue")).andReturn();
        String episodeId = JsonPath.read(queue.getResponse().getContentAsString(), "$.episodes[0].id");

        mvc.perform(patch("/api/episodes/" + episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"played\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.played").value(true));

        mvc.perform(get("/api/queue"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.episodes[0].title").value("Episódio do meio"));

        mvc.perform(post("/api/podcasts/" + podcastId + "/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.podcast.unplayedCount").value(2));

        mvc.perform(get("/api/queue")).andExpect(jsonPath("$.total").value(2));

        mvc.perform(post("/api/podcasts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedUrl\":\"https://EXAMPLE.com:443/feed.xml\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(podcastId));

        mvc.perform(post("/api/podcasts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedUrl\":\"http://127.0.0.1/secret\"}"))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(delete("/api/podcasts/" + podcastId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/queue")).andExpect(jsonPath("$.total").value(0));
    }

    @TestConfiguration
    static class Stubs {

        @Bean
        @Primary
        FeedFetcher feedFetcher() {
            return uri -> {
                try {
                    return Files.readAllBytes(Path.of("src/test/resources/sample-feed.xml"));
                } catch (java.io.IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            };
        }
    }
}
