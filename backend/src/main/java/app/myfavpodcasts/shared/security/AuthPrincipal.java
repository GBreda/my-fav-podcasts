package app.myfavpodcasts.shared.security;

import java.util.UUID;

public record AuthPrincipal(UUID id, String email, String displayName) {
}
