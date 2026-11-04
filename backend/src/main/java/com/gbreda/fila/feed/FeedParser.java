package com.gbreda.fila.feed;

import com.gbreda.fila.web.InvalidFeedException;
import com.rometools.modules.itunes.EntryInformation;
import com.rometools.modules.itunes.FeedInformation;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.jdom2.Element;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class FeedParser {

    static final int MAX_EPISODES = 150;
    private static final String ITUNES = "http://www.itunes.com/dtds/podcast-1.0.dtd";

    private final FeedUrlValidator urls = new FeedUrlValidator();

    public ParsedFeed parse(String feedUrl, byte[] body) {
        SyndFeed feed = read(body);
        String artwork = firstNonBlank(
                itunesImage(feed.getForeignMarkup()),
                moduleImage(feed.getModule(ITUNES)),
                feed.getImage() == null ? null : feed.getImage().getUrl());
        String author = firstNonBlank(moduleAuthor(feed.getModule(ITUNES)), feed.getAuthor());
        List<ParsedEpisode> episodes = new ArrayList<>();
        for (SyndEntry entry : feed.getEntries()) {
            ParsedEpisode episode = episode(feedUrl, artwork, entry);
            if (episode != null) {
                episodes.add(episode);
            }
        }
        episodes.sort(Comparator.comparing(ParsedEpisode::publishedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        if (episodes.size() > MAX_EPISODES) {
            episodes = new ArrayList<>(episodes.subList(0, MAX_EPISODES));
        }
        return new ParsedFeed(
                clip(firstNonBlank(feed.getTitle(), "Podcast sem nome"), 500),
                clip(author, 500),
                plain(firstNonBlank(feed.getDescription(), moduleSummary(feed.getModule(ITUNES))), 4000),
                clip(artwork, 1500),
                List.copyOf(episodes));
    }

    private ParsedEpisode episode(String feedUrl, String feedArtwork, SyndEntry entry) {
        String audio = audioUrl(feedUrl, entry.getEnclosures());
        if (audio == null) {
            return null;
        }
        EntryInformation itunes = entry.getModule(ITUNES) instanceof EntryInformation info ? info : null;
        String artwork = firstNonBlank(
                itunesImage(entry.getForeignMarkup()),
                itunes == null || itunes.getImage() == null ? null : itunes.getImage().toString(),
                feedArtwork);
        String description = plain(firstNonBlank(
                content(entry),
                entry.getDescription() == null ? null : entry.getDescription().getValue(),
                itunes == null ? null : itunes.getSummary()), 4000);
        return new ParsedEpisode(
                clip(firstNonBlank(entry.getUri(), entry.getLink(), audio), 2048),
                clip(firstNonBlank(entry.getTitle(), "Episódio sem título"), 500),
                description,
                clip(audio, 2000),
                clip(artwork, 1500),
                published(entry),
                duration(itunes, entry.getForeignMarkup()));
    }

    private static SyndFeed read(byte[] body) {
        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(body))) {
            return new SyndFeedInput().build(reader);
        } catch (FeedException | IOException exception) {
            throw new InvalidFeedException("Não consegui ler esse feed.");
        }
    }

    private String audioUrl(String feedUrl, List<SyndEnclosure> enclosures) {
        if (enclosures == null || enclosures.isEmpty()) {
            return null;
        }
        String fallback = null;
        for (SyndEnclosure enclosure : enclosures) {
            if (enclosure.getUrl() == null || enclosure.getUrl().isBlank()) {
                continue;
            }
            String type = enclosure.getType() == null ? "" : enclosure.getType().toLowerCase(Locale.ROOT);
            boolean audio = type.startsWith("audio/") || looksLikeAudio(enclosure.getUrl());
            boolean generic = type.isBlank() || type.equals("application/octet-stream") || type.equals("application/ogg");
            if (!audio && !generic) {
                continue;
            }
            String resolved = resolve(feedUrl, enclosure.getUrl());
            if (resolved == null) {
                continue;
            }
            if (audio) {
                return resolved;
            }
            if (fallback == null) {
                fallback = resolved;
            }
        }
        return fallback;
    }

    private String resolve(String feedUrl, String raw) {
        try {
            URI resolved = URI.create(feedUrl).resolve(raw.trim());
            return urls.normalize(urls.parse(resolved.toString()));
        } catch (IllegalArgumentException | InvalidFeedException exception) {
            return null;
        }
    }

    private static boolean looksLikeAudio(String url) {
        String path = url.toLowerCase(Locale.ROOT);
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        return path.endsWith(".mp3")
                || path.endsWith(".m4a")
                || path.endsWith(".aac")
                || path.endsWith(".ogg")
                || path.endsWith(".wav")
                || path.endsWith(".opus");
    }

    private static Integer duration(EntryInformation itunes, List<Element> foreign) {
        if (itunes != null && itunes.getDuration() != null) {
            long seconds = itunes.getDuration().getMilliseconds() / 1000;
            if (seconds > 0) {
                return (int) Math.min(seconds, Integer.MAX_VALUE);
            }
        }
        return parseDuration(foreignText(foreign, "duration"));
    }

    static Integer parseDuration(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.matches("\\d+")) {
            long seconds = Long.parseLong(value);
            if (seconds > 100_000) {
                seconds = seconds / 1000;
            }
            return seconds <= 0 ? null : (int) Math.min(seconds, Integer.MAX_VALUE);
        }
        String[] parts = value.split(":");
        if (parts.length < 2 || parts.length > 3) {
            return null;
        }
        try {
            int[] nums = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                nums[i] = Integer.parseInt(parts[i].trim());
            }
            int seconds = parts.length == 2
                    ? nums[0] * 60 + nums[1]
                    : nums[0] * 3600 + nums[1] * 60 + nums[2];
            return seconds <= 0 ? null : seconds;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Instant published(SyndEntry entry) {
        Date date = entry.getPublishedDate() != null ? entry.getPublishedDate() : entry.getUpdatedDate();
        return date == null ? null : date.toInstant();
    }

    private static String content(SyndEntry entry) {
        if (entry.getContents() == null || entry.getContents().isEmpty() || entry.getContents().getFirst() == null) {
            return null;
        }
        return entry.getContents().getFirst().getValue();
    }

    private static String itunesImage(List<Element> foreign) {
        String href = foreignAttr(foreign, "image", "href");
        return href != null ? href : foreignText(foreign, "image");
    }

    private static String foreignText(List<Element> elements, String name) {
        if (elements == null) {
            return null;
        }
        for (Element element : elements) {
            if (name.equalsIgnoreCase(element.getName())) {
                String text = element.getTextTrim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private static String foreignAttr(List<Element> elements, String name, String attr) {
        if (elements == null) {
            return null;
        }
        for (Element element : elements) {
            if (name.equalsIgnoreCase(element.getName())) {
                String value = element.getAttributeValue(attr);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private static String moduleImage(Object module) {
        if (module instanceof FeedInformation info && info.getImage() != null) {
            return info.getImage().toString();
        }
        return null;
    }

    private static String moduleAuthor(Object module) {
        if (module instanceof FeedInformation info) {
            return info.getAuthor();
        }
        return null;
    }

    private static String moduleSummary(Object module) {
        if (module instanceof FeedInformation info) {
            return info.getSummary();
        }
        return null;
    }

    private static String plain(String html, int max) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String text = Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
        return clip(text, max);
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max - 1).trim() + "…";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
