package app.myfavpodcasts.shared.domain;

public final class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String code) {
        super(code);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
