package app.myfavpodcasts.shared.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlainText {

    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x?)([0-9a-fA-F]+);");

    private PlainText() {
    }

    public static String of(String html, int max) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String text = html
                .replaceAll("(?i)<br\\s*/?>", " ")
                .replaceAll("(?i)</p>", " ")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
        text = decodeNumericEntities(text).replace("&amp;", "&");
        text = text.replaceAll("\\s+", " ").trim();
        return clip(text, max);
    }

    private static String decodeNumericEntities(String text) {
        Matcher matcher = NUMERIC_ENTITY.matcher(text);
        StringBuilder decoded = new StringBuilder();
        while (matcher.find()) {
            int codePoint = matcher.group(1).isEmpty()
                    ? Integer.parseInt(matcher.group(2))
                    : Integer.parseInt(matcher.group(2), 16);
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(Character.toString(codePoint)));
        }
        matcher.appendTail(decoded);
        return decoded.toString();
    }

    public static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }

    public static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
