package app.myfavpodcasts.identity.infrastructure;

import app.myfavpodcasts.identity.domain.PasswordResetMail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingPasswordResetMail implements PasswordResetMail {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetMail.class);

    @Override
    public String delivery() {
        return "log";
    }

    @Override
    public void send(String email, String resetUrl, String locale) {
        log.info("Link de redefinição de senha para {}: {}", email, resetUrl);
    }
}
