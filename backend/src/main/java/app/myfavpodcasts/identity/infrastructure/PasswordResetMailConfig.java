package app.myfavpodcasts.identity.infrastructure;

import app.myfavpodcasts.identity.domain.PasswordResetMail;
import app.myfavpodcasts.shared.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class PasswordResetMailConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.mail.host")
    PasswordResetMail smtpPasswordResetMail(JavaMailSender sender, AppProperties properties) {
        return new SmtpPasswordResetMail(sender, properties);
    }

    @Bean
    @ConditionalOnMissingBean(PasswordResetMail.class)
    PasswordResetMail loggingPasswordResetMail() {
        return new LoggingPasswordResetMail();
    }
}
