package app.myfavpodcasts.identity.domain;

import app.myfavpodcasts.shared.domain.DomainException;

public record DisplayName(String value) {

    public DisplayName {
        if (value == null) {
            throw new DomainException("VALIDATION");
        }
        value = value.trim().replaceAll("\\s+", " ");
        if (value.length() < 2 || value.length() > 40) {
            throw new DomainException("VALIDATION");
        }
    }
}
