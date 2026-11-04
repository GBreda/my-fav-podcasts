package app.myfavpodcasts.identity.domain;

import app.myfavpodcasts.shared.domain.DomainException;

public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void check(String raw) {
        if (raw == null || raw.length() < 8 || raw.length() > 72) {
            throw new DomainException("VALIDATION");
        }
    }
}
