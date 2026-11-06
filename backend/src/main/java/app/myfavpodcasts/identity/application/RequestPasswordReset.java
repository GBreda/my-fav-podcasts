package app.myfavpodcasts.identity.application;

import app.myfavpodcasts.identity.domain.Email;
import app.myfavpodcasts.identity.domain.PasswordReset;
import app.myfavpodcasts.identity.domain.PasswordResetMail;
import app.myfavpodcasts.identity.domain.PasswordResetRepository;
import app.myfavpodcasts.identity.domain.ResetTokens;
import app.myfavpodcasts.identity.domain.UserRepository;
import app.myfavpodcasts.shared.config.AppProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestPasswordReset {

    public static final Duration TTL = Duration.ofMinutes(30);

    private static final Logger log = LoggerFactory.getLogger(RequestPasswordReset.class);

    private final UserRepository users;
    private final PasswordResetRepository resets;
    private final PasswordResetMail mail;
    private final AppProperties properties;

    public RequestPasswordReset(
            UserRepository users,
            PasswordResetRepository resets,
            PasswordResetMail mail,
            AppProperties properties
    ) {
        this.users = users;
        this.resets = resets;
        this.mail = mail;
        this.properties = properties;
    }

    @Transactional
    public String execute(String email, String locale) {
        String language = "en-US".equals(locale) ? "en-US" : "pt-BR";
        users.findByEmail(new Email(email)).ifPresent(user -> {
            String raw = ResetTokens.newRaw();
            resets.deleteByUserId(user.id());
            resets.save(new PasswordReset(
                    UUID.randomUUID(),
                    user.id(),
                    ResetTokens.hash(raw),
                    Instant.now().plus(TTL),
                    null));
            String url = properties.publicBaseUrl()
                    + "/" + language
                    + "/reset?token=" + URLEncoder.encode(raw, StandardCharsets.UTF_8);
            try {
                mail.send(user.email().value(), url, language);
            } catch (RuntimeException exception) {
                log.warn("Falha ao enviar o e-mail de redefinição para {}. Link: {}", user.email().value(), url, exception);
            }
        });
        return mail.delivery();
    }
}
