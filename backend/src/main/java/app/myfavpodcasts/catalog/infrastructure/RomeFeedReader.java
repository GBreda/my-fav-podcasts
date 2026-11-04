package app.myfavpodcasts.catalog.infrastructure;

import app.myfavpodcasts.catalog.domain.FeedEpisode;
import app.myfavpodcasts.catalog.domain.FeedReader;
import app.myfavpodcasts.catalog.domain.FeedSnapshot;
import app.myfavpodcasts.shared.domain.DomainException;
import app.myfavpodcasts.shared.domain.SafeUrls;
import app.myfavpodcasts.shared.text.PlainText;
import com.rometools.modules.itunes.EntryInformation;
import com.rometools.modules.itunes.FeedInformation;
import com.rometools.modules.itunes.ITunes;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RomeFeedReader implements FeedReader {

    static final int MAX_BYTES = 16_000_000;
    private static final int MAX_EPISODES = 40;
    private static final byte[] ITEM_END = "</item>".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ENTRY_END = "</entry>".getBytes(StandardCharsets.US_ASCII);
    private static final String USER_AGENT = "Fila/1.0 (podcast queue; +https://github.com)";

    private static final Logger log = LoggerFactory.getLogger(RomeFeedReader.class);

    private final HttpClient http;

    public RomeFeedReader(HttpClient http) {
        this.http = http;
    }

    @Override
    public FeedSnapshot fetch(URI feedUrl) {
        URI current = URI.create(SafeUrls.normalize(feedUrl.toString()));
        for (int hop = 0; hop < 4; hop++) {
            SafeUrls.requirePublic(current.toString());
            HttpRequest request = HttpRequest.newBuilder(current)
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
                    .GET()
                    .build();
            HttpResponse<InputStream> response;
            try {
                response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            } catch (IOException exception) {
                log.warn("Feed fetch failed for {}: {}", current.getHost(), exception.toString());
                throw new DomainException("FEED_UNREADABLE");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new DomainException("FEED_UNREADABLE");
            }
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                closeQuietly(response.body());
                String location = response.headers().firstValue("location").orElse("");
                if (location.isBlank()) {
                    throw new DomainException("FEED_UNREADABLE");
                }
                current = current.resolve(location);
                continue;
            }
            if (status != 200) {
                closeQuietly(response.body());
                throw new DomainException("FEED_UNREADABLE");
            }
            try (InputStream body = response.body()) {
                return parse(limit(body));
            } catch (IOException exception) {
                log.warn("Feed body failed for {}: {}", current.getHost(), exception.toString());
                throw new DomainException("FEED_UNREADABLE");
            } catch (DomainException exception) {
                log.warn("Feed unreadable for {}: {}", current.getHost(), exception.code());
                throw exception;
            }
        }
        throw new DomainException("FEED_UNREADABLE");
    }

    public static FeedSnapshot parse(byte[] xml) {
        SyndFeedInput input = new SyndFeedInput();
        input.setAllowDoctypes(false);
        SyndFeed feed;
        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(xml))) {
            feed = input.build(reader);
        } catch (IOException | FeedException | IllegalArgumentException exception) {
            log.warn("Feed parse failed: {}", exception.toString());
            throw new DomainException("FEED_UNREADABLE");
        }
        FeedInformation channel = module(feed.getModule(ITunes.URI), FeedInformation.class);
        List<FeedEpisode> episodes = new ArrayList<>();
        for (SyndEntry entry : feed.getEntries()) {
            EntryInformation item = module(entry.getModule(ITunes.URI), EntryInformation.class);
            String audio = audioUrl(entry);
            Instant published = publishedAt(entry);
            if (audio == null || published == null) {
                continue;
            }
            String guid = PlainText.first(entry.getUri(), audio);
            String summary = PlainText.of(PlainText.first(
                    item == null ? "" : item.getSummary(),
                    entry.getDescription() == null ? "" : entry.getDescription().getValue()), 400);
            episodes.add(new FeedEpisode(
                    PlainText.clip(guid, 500),
                    PlainText.clip(PlainText.first(entry.getTitle(), "Episódio"), 240),
                    summary,
                    PlainText.clip(audio, 2000),
                    PlainText.clip(image(item), 1000),
                    published,
                    duration(item)));
        }
        episodes.sort(Comparator.comparing(FeedEpisode::publishedAt).reversed());
        if (episodes.size() > MAX_EPISODES) {
            episodes = List.copyOf(episodes.subList(0, MAX_EPISODES));
        }
        return new FeedSnapshot(
                PlainText.clip(PlainText.first(feed.getTitle(), "Podcast"), 240),
                PlainText.clip(PlainText.first(channel == null ? "" : channel.getAuthor(), feed.getAuthor(), channel == null ? "" : channel.getOwnerName()), 160),
                PlainText.of(PlainText.first(channel == null ? "" : channel.getSummary(), feed.getDescription()), 1200),
                PlainText.clip(PlainText.first(image(channel), feed.getImage() == null ? "" : feed.getImage().getUrl()), 1000),
                episodes);
    }

    private static String audioUrl(SyndEntry entry) {
        for (SyndEnclosure enclosure : entry.getEnclosures()) {
            if (enclosure.getUrl() == null || enclosure.getUrl().isBlank()) {
                continue;
            }
            String type = enclosure.getType() == null ? "" : enclosure.getType().toLowerCase(Locale.ROOT);
            String url = enclosure.getUrl().toLowerCase(Locale.ROOT);
            boolean audioType = type.startsWith("audio");
            boolean audioExt = url.matches(".*\\.(mp3|m4a|aac|ogg|opus|wav)(\\?.*)?$");
            if (audioType || audioExt) {
                return enclosure.getUrl().trim();
            }
        }
        return null;
    }

    private static Instant publishedAt(SyndEntry entry) {
        if (entry.getPublishedDate() != null) {
            return entry.getPublishedDate().toInstant();
        }
        if (entry.getUpdatedDate() != null) {
            return entry.getUpdatedDate().toInstant();
        }
        return null;
    }

    private static Integer duration(EntryInformation item) {
        if (item == null || item.getDuration() == null) {
            return null;
        }
        long milliseconds = item.getDuration().getMilliseconds();
        if (milliseconds <= 0) {
            return null;
        }
        return (int) Math.min(milliseconds / 1000, Integer.MAX_VALUE);
    }

    private static String image(ITunes itunes) {
        if (itunes == null) {
            return "";
        }
        if (itunes.getImage() != null) {
            return itunes.getImage().toString();
        }
        return itunes.getImageUri() == null ? "" : itunes.getImageUri();
    }

    private static <T> T module(Object module, Class<T> type) {
        return type.isInstance(module) ? type.cast(module) : null;
    }

    /**
     * Podcast feeds often list years of episodes. The queue only keeps the newest
     * {@value #MAX_EPISODES}, so reading stops at that many items and closes the document.
     */
    public static byte[] limit(InputStream input) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        int items = 0;
        int entries = 0;
        int scanned = 0;
        boolean capped = false;
        while ((read = input.read(buffer)) >= 0) {
            raw.write(buffer, 0, read);
            byte[] data = raw.toByteArray();
            while (scanned <= data.length - ITEM_END.length) {
                if (matches(data, scanned, ITEM_END)) {
                    items++;
                    scanned += ITEM_END.length;
                    if (items >= MAX_EPISODES) {
                        capped = true;
                        break;
                    }
                    continue;
                }
                if (matches(data, scanned, ENTRY_END)) {
                    entries++;
                    scanned += ENTRY_END.length;
                    if (entries >= MAX_EPISODES) {
                        capped = true;
                        break;
                    }
                    continue;
                }
                scanned++;
            }
            if (capped || data.length >= MAX_BYTES) {
                break;
            }
        }
        byte[] data = raw.toByteArray();
        if (data.length == 0) {
            throw new DomainException("FEED_UNREADABLE");
        }
        if (!capped && data.length < MAX_BYTES) {
            return data;
        }
        byte[] marker = items >= entries ? ITEM_END : ENTRY_END;
        int kept = Math.min(MAX_EPISODES, items >= entries ? items : entries);
        int cut = endOfOccurrence(data, marker, kept);
        if (cut < 0) {
            throw new DomainException("FEED_UNREADABLE");
        }
        return closeDocument(Arrays.copyOf(data, cut));
    }

    private static byte[] closeDocument(byte[] head) {
        String opening = new String(head, 0, Math.min(head.length, 500), StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
        String suffix = opening.contains("<feed") && !opening.contains("<rss") ? "\n</feed>\n" : "\n</channel>\n</rss>\n";
        byte[] tail = suffix.getBytes(StandardCharsets.US_ASCII);
        byte[] closed = Arrays.copyOf(head, head.length + tail.length);
        System.arraycopy(tail, 0, closed, head.length, tail.length);
        return closed;
    }

    private static boolean matches(byte[] data, int offset, byte[] needle) {
        for (int i = 0; i < needle.length; i++) {
            byte value = data[offset + i];
            if (value >= 'A' && value <= 'Z') {
                value += 32;
            }
            if (value != needle[i]) {
                return false;
            }
        }
        return true;
    }

    private static int endOfOccurrence(byte[] data, byte[] needle, int occurrence) {
        int seen = 0;
        for (int i = 0; i <= data.length - needle.length; i++) {
            if (matches(data, i, needle)) {
                seen++;
                if (seen == occurrence) {
                    return i + needle.length;
                }
            }
        }
        return -1;
    }

    private static void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // The next hop only needs the location header.
        }
    }
}
