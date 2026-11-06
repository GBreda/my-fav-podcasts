package app.myfavpodcasts.identity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.myfavpodcasts.identity.domain.Email;
import app.myfavpodcasts.identity.domain.PasswordReset;
import app.myfavpodcasts.identity.domain.PasswordResetMail;
import app.myfavpodcasts.identity.domain.PasswordResetRepository;
import app.myfavpodcasts.identity.domain.ResetTokens;
import app.myfavpodcasts.identity.domain.UserRepository;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
class PasswordResetFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordResetRepository resets;

    @MockitoBean
    private PasswordResetMail mail;

    private final List<String> links = new ArrayList<>();

    @BeforeEach
    void mailCapturesTheLink() {
        links.clear();
        when(mail.delivery()).thenReturn("log");
        doAnswer(invocation -> {
            links.add(invocation.getArgument(1));
            return null;
        }).when(mail).send(any(), any(), any());
    }

    @Test
    void resetsThePasswordWithoutRevealingWhetherTheEmailExists() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nao-e-email","locale":"pt-BR"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ninguem@example.com","locale":"pt-BR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery").value("log"));
        assertTrue(links.isEmpty());

        String session = tokenOf(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"senha-forte","displayName":"Ana"}
                                """))
                .andExpect(status().isCreated())
                .andReturn());

        var user = users.findByEmail(new Email("ana@example.com")).orElseThrow();
        resets.save(new PasswordReset(
                UUID.randomUUID(),
                user.id(),
                ResetTokens.hash("vencido"),
                Instant.now().minusSeconds(120),
                null));
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"vencido","password":"senha-nova"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"Ana@Example.com","locale":"pt-BR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery").value("log"));
        String first = tokenFrom(links.get(0));
        assertTrue(links.get(0).startsWith("http://localhost:3000/pt-BR/reset?token="));
        assertTrue(resets.findByTokenHash(ResetTokens.hash(first)).isPresent());
        assertFalse(resets.findByTokenHash(first).isPresent());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","locale":"en-US"}
                                """))
                .andExpect(status().isOk());
        String second = tokenFrom(links.get(1));
        assertTrue(links.get(1).startsWith("http://localhost:3000/en-US/reset?token="));
        assertFalse(resets.findByTokenHash(ResetTokens.hash(first)).isPresent());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"senha-nova"}
                                """.formatted(first)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"curta"}
                                """.formatted(second)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"senha-nova"}
                                """.formatted(second)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"senha-nova"}
                                """.formatted(second)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"senha-forte"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"senha-nova"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","locale":"pt-BR"}
                                """))
                .andExpect(status().isOk());
        String third = tokenFrom(links.get(2));

        mockMvc.perform(post("/api/auth/password")
                        .header("Authorization", "Bearer " + session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"senha-nova","nextPassword":"terceira-senha"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"outra-senha"}
                                """.formatted(third)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"terceira-senha"}
                                """))
                .andExpect(status().isOk());
    }

    private static String tokenOf(MvcResult result) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private static String tokenFrom(String url) {
        String query = URI.create(url).getRawQuery();
        String raw = query.substring(query.indexOf("token=") + "token=".length());
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }
}
