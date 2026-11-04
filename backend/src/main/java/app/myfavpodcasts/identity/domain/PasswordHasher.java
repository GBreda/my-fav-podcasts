package app.myfavpodcasts.identity.domain;

public interface PasswordHasher {

    String hash(String raw);

    boolean matches(String raw, String hashed);
}
