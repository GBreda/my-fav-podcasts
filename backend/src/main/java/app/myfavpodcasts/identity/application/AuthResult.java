package app.myfavpodcasts.identity.application;

import app.myfavpodcasts.identity.domain.User;

public record AuthResult(String token, User user) {
}
