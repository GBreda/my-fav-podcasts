package app.myfavpodcasts.identity.domain;

public interface SessionTokens {

    String issue(User user);
}
