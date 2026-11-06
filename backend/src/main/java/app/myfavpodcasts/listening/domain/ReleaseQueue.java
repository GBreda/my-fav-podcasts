package app.myfavpodcasts.listening.domain;

import java.util.Comparator;
import java.util.List;

public final class ReleaseQueue {

    private ReleaseQueue() {
    }

    public static List<QueueItem> compose(List<QueueItem> items, QueueFilter filter, ReleaseOrder order, int limit) {
        Comparator<QueueItem> byDate = Comparator.comparing(QueueItem::publishedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<QueueItem> byTitle = Comparator.comparing(item -> item.title() == null ? "" : item.title(), String.CASE_INSENSITIVE_ORDER);
        Comparator<QueueItem> full = order == ReleaseOrder.NEWEST_FIRST
                ? byDate.reversed().thenComparing(byTitle).thenComparing(QueueItem::episodeId)
                : byDate.thenComparing(byTitle).thenComparing(QueueItem::episodeId);
        return items.stream()
                .filter(item -> filter == QueueFilter.ALL || !item.completed())
                .sorted(full)
                .limit(Math.max(0, limit))
                .toList();
    }
}
