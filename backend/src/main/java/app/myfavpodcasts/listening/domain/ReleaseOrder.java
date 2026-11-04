package app.myfavpodcasts.listening.domain;

public enum ReleaseOrder {
    NEWEST_FIRST,
    OLDEST_FIRST;

    public static ReleaseOrder from(String raw) {
        return "oldest".equalsIgnoreCase(raw) ? OLDEST_FIRST : NEWEST_FIRST;
    }
}
