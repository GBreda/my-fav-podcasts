package app.myfavpodcasts.listening.application;

import app.myfavpodcasts.listening.domain.ListeningRepository;
import app.myfavpodcasts.listening.domain.QueueFilter;
import app.myfavpodcasts.listening.domain.QueueItem;
import app.myfavpodcasts.listening.domain.ReleaseOrder;
import app.myfavpodcasts.listening.domain.ReleaseQueue;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetQueue {

    private static final int LIMIT = 120;

    private final ListeningRepository listening;

    public GetQueue(ListeningRepository listening) {
        this.listening = listening;
    }

    public List<QueueItem> execute(UUID userId, QueueFilter filter, ReleaseOrder order) {
        return ReleaseQueue.compose(listening.items(userId), filter, order, LIMIT);
    }
}
