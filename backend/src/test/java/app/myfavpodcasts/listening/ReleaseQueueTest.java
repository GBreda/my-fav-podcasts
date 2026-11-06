package app.myfavpodcasts.listening;

import static org.assertj.core.api.Assertions.assertThat;

import app.myfavpodcasts.listening.domain.QueueFilter;
import app.myfavpodcasts.listening.domain.QueueItem;
import app.myfavpodcasts.listening.domain.ReleaseOrder;
import app.myfavpodcasts.listening.domain.ReleaseQueue;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReleaseQueueTest {

    @Test
    void ordersUnplayedEpisodesByReleaseDateNewestFirst() {
        QueueItem older = item("Antigo", "2026-11-01T10:00:00Z", false);
        QueueItem newer = item("Novo", "2026-11-04T10:00:00Z", false);
        QueueItem played = item("Ouvido", "2026-11-05T10:00:00Z", true);
        QueueItem sameDay = item("Zebra", "2026-11-04T10:00:00Z", false);

        List<QueueItem> queue = ReleaseQueue.compose(
                List.of(older, played, sameDay, newer),
                QueueFilter.UNPLAYED,
                ReleaseOrder.NEWEST_FIRST,
                10);

        assertThat(queue).extracting(QueueItem::title).containsExactly("Novo", "Zebra", "Antigo");
    }

    @Test
    void oldestFirstKeepsPlayedWhenAskedAndRespectsLimit() {
        QueueItem first = item("A", "2026-11-01T00:00:00Z", false);
        QueueItem second = item("B", "2026-11-02T00:00:00Z", true);
        QueueItem third = item("C", "2026-11-03T00:00:00Z", false);

        List<QueueItem> queue = ReleaseQueue.compose(
                List.of(third, first, second),
                QueueFilter.ALL,
                ReleaseOrder.OLDEST_FIRST,
                2);

        assertThat(queue).extracting(QueueItem::title).containsExactly("A", "B");
    }

    private static QueueItem item(String title, String publishedAt, boolean completed) {
        return new QueueItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Show",
                title,
                "",
                "https://cdn.example.com/" + title + ".mp3",
                "",
                Instant.parse(publishedAt),
                60,
                0,
                completed);
    }
}
