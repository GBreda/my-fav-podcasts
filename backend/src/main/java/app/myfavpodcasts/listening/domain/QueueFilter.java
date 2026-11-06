package app.myfavpodcasts.listening.domain;

public enum QueueFilter {
    UNPLAYED,
    ALL;

    public static QueueFilter from(String raw) {
        return "all".equalsIgnoreCase(raw) ? ALL : UNPLAYED;
    }
}
