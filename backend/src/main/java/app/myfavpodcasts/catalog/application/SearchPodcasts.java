package app.myfavpodcasts.catalog.application;

import app.myfavpodcasts.catalog.domain.PodcastDirectory;
import app.myfavpodcasts.catalog.domain.PodcastHit;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class SearchPodcasts {

    private final PodcastDirectory directory;

    public SearchPodcasts(PodcastDirectory directory) {
        this.directory = directory;
    }

    public List<PodcastHit> execute(String query, String country) {
        String safeCountry = country != null && country.matches("(?i)[a-z]{2}")
                ? country.toUpperCase(Locale.ROOT)
                : "BR";
        return directory.search(query == null ? "" : query.trim(), safeCountry);
    }
}
