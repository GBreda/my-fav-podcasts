package com.gbreda.fila.feed;

import com.gbreda.fila.web.InvalidFeedException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class FeedUrlValidator {

    public URI parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidFeedException("Informe o endereço do feed.");
        }
        URI uri;
        try {
            uri = URI.create(raw.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidFeedException("Endereço do feed inválido.");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
            throw new InvalidFeedException("O feed precisa ser um endereço http ou https.");
        }
        if (uri.getUserInfo() != null) {
            throw new InvalidFeedException("Endereço do feed inválido.");
        }
        String host = hostOf(uri);
        if (host == null) {
            throw new InvalidFeedException("Endereço do feed inválido.");
        }
        if (blockedName(host) || (isLiteralAddress(host) && blockedAddress(literal(host)))) {
            throw new InvalidFeedException("Esse endereço de feed não é permitido.");
        }
        return uri;
    }

    public String normalize(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = hostOf(uri).toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        boolean defaultPort = port == -1
                || ("https".equals(scheme) && port == 443)
                || ("http".equals(scheme) && port == 80);
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        return scheme + "://" + host + (defaultPort ? "" : ":" + port) + path + query;
    }

    public void assertPublicDns(URI uri) {
        String host = hostOf(uri);
        if (isLiteralAddress(host)) {
            return;
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException exception) {
            throw new InvalidFeedException("Não encontrei esse endereço.");
        }
        if (addresses.length == 0) {
            throw new InvalidFeedException("Não encontrei esse endereço.");
        }
        for (InetAddress address : addresses) {
            if (blockedAddress(address)) {
                throw new InvalidFeedException("Esse endereço de feed não é permitido.");
            }
        }
    }

    private static String hostOf(URI uri) {
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return null;
        }
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static boolean blockedName(String host) {
        String value = host.toLowerCase(Locale.ROOT);
        return value.equals("localhost")
                || value.endsWith(".localhost")
                || value.endsWith(".local")
                || value.endsWith(".internal");
    }

    private static boolean isLiteralAddress(String host) {
        return host.indexOf(':') >= 0 || host.chars().allMatch(ch -> Character.isDigit(ch) || ch == '.');
    }

    private static InetAddress literal(String host) {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException exception) {
            throw new InvalidFeedException("Endereço do feed inválido.");
        }
    }

    private static boolean blockedAddress(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress();
    }
}
