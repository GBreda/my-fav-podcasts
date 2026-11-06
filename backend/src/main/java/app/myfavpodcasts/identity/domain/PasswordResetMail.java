package app.myfavpodcasts.identity.domain;

public interface PasswordResetMail {

    String delivery();

    void send(String email, String resetUrl, String locale);
}
