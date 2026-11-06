package app.myfavpodcasts.catalog.domain;

import java.net.URI;

public interface FeedReader {

    FeedSnapshot fetch(URI feedUrl);
}
