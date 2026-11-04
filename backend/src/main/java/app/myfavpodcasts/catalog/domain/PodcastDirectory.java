package app.myfavpodcasts.catalog.domain;

import java.util.List;

public interface PodcastDirectory {

    List<PodcastHit> search(String query, String country);
}
