package app.myfavpodcasts.identity.domain;

import app.myfavpodcasts.shared.domain.DomainException;
import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        if (value == null) {
            throw new DomainException("VALIDATION");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > 255 || !PATTERN.matcher(value).matches()) {
            throw new DomainException("VALIDATION");
        }
    }
}
