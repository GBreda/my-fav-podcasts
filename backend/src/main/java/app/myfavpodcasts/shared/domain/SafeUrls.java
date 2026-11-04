package app.myfavpodcasts.shared.domain;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

public final class SafeUrls {

    private SafeUrls() {
    }

    public static String normalize(String raw) {
        URI uri = structure(raw);
        if (blockedLiteral(uri.getHost())) {
            throw new DomainException("FEED_URL_BLOCKED");
        }
        return canonical(uri);
    }

    public static void requirePublic(String normalizedUrl) {
        URI uri = URI.create(normalizedUrl);
        String host = uri.getHost();
        if (host == null) {
            throw new DomainException("FEED_URL_BLOCKED");
        }
        if (isIpLiteral(host)) {
            if (blockedLiteral(host)) {
                throw new DomainException("FEED_URL_BLOCKED");
            }
            return;
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw new DomainException("FEED_UNREADABLE");
            }
            for (InetAddress address : addresses) {
                if (blocked(address)) {
                    throw new DomainException("FEED_URL_BLOCKED");
                }
            }
        } catch (UnknownHostException exception) {
            throw new DomainException("FEED_UNREADABLE");
        }
    }

    private static URI structure(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException("FEED_URL_BLOCKED");
        }
        URI uri;
        try {
            uri = URI.create(raw.trim());
        } catch (IllegalArgumentException exception) {
            throw new DomainException("FEED_URL_BLOCKED");
        }
        if (uri.getScheme() == null || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new DomainException("FEED_URL_BLOCKED");
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http")) {
            throw new DomainException("FEED_URL_BLOCKED");
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("localhost")
                || host.endsWith(".localhost")
                || host.endsWith(".local")
                || host.equals("metadata.google.internal")
                || host.equals("0.0.0.0")) {
            throw new DomainException("FEED_URL_BLOCKED");
        }
        return uri;
    }

    private static String canonical(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "" : uri.getRawPath();
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        StringBuilder builder = new StringBuilder();
        builder.append(scheme).append("://").append(host);
        boolean defaultPort = (scheme.equals("https") && port == 443) || (scheme.equals("http") && port == 80);
        if (port != -1 && !defaultPort) {
            builder.append(':').append(port);
        }
        builder.append(path);
        if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
            builder.append('?').append(uri.getRawQuery());
        }
        return builder.toString();
    }

    private static boolean isIpLiteral(String host) {
        return host.chars().allMatch(ch -> Character.isDigit(ch) || ch == '.')
                || host.indexOf(':') >= 0;
    }

    private static boolean blockedLiteral(String host) {
        try {
            if (!isIpLiteral(host)) {
                return false;
            }
            return blocked(InetAddress.getByName(host));
        } catch (UnknownHostException exception) {
            return true;
        }
    }

    private static boolean blocked(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            if (first == 0 || first == 10 || first == 127 || first >= 224) {
                return true;
            }
            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }
            if (first == 169 && second == 254) {
                return true;
            }
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            return first == 192 && second == 168;
        }
        if (bytes.length == 16) {
            int first = bytes[0] & 0xff;
            return (first & 0xfe) == 0xfc;
        }
        return false;
    }
}
