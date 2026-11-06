package app.myfavpodcasts.identity.infrastructure;

import app.myfavpodcasts.identity.domain.PasswordResetMail;
import app.myfavpodcasts.shared.config.AppProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

public class SmtpPasswordResetMail implements PasswordResetMail {

    private final JavaMailSender sender;
    private final AppProperties properties;

    public SmtpPasswordResetMail(JavaMailSender sender, AppProperties properties) {
        this.sender = sender;
        this.properties = properties;
    }

    @Override
    public String delivery() {
        return "email";
    }

    @Override
    public void send(String email, String resetUrl, String locale) {
        boolean english = "en-US".equals(locale);
        sender.send(message -> {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(email);
            helper.setFrom(properties.mailFrom());
            helper.setSubject(english ? "Reset your Fila password" : "Redefinir a senha da Fila");
            helper.setText(english ? englishBody(resetUrl) : portugueseBody(resetUrl));
        });
    }

    private static String portugueseBody(String resetUrl) {
        return """
                Alguém pediu uma senha nova para a sua conta na Fila.

                Abre este link para escolher outra senha. Ele vale por 30 minutos:
                %s

                Se não foi você, pode ignorar este e-mail. A senha atual continua valendo.
                """.formatted(resetUrl);
    }

    private static String englishBody(String resetUrl) {
        return """
                Someone asked for a new password for your Fila account.

                Open this link to choose another password. It lasts 30 minutes:
                %s

                If this wasn't you, ignore this email. Your current password still works.
                """.formatted(resetUrl);
    }
}
