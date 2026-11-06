package com.gbreda.fila.feed;

import com.gbreda.fila.web.InvalidFeedException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import org.springframework.stereotype.Component;

@Component
public class HttpFeedFetcher implements FeedFetcher {

    static final int MAX_COMPRESSED = 8_000_000;
    static final int MAX_XML = 20_000_000;
    private static final int MAX_REDIRECTS = 5;

    private final HttpClient http;
    private final FeedUrlValidator urls;

    public HttpFeedFetcher(HttpClient http, FeedUrlValidator urls) {
        this.http = http;
        this.urls = urls;
    }

    @Override
    public byte[] fetch(URI uri) {
        URI current = uri;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            urls.assertPublicDns(current);
            HttpRequest request = HttpRequest.newBuilder(current)
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Fila/0.1 (podcast queue)")
                    .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
                    .header("Accept-Encoding", "gzip")
                    .GET()
                    .build();
            HttpResponse<byte[]> response;
            try {
                response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new InvalidFeedException("A leitura do feed foi interrompida.");
            } catch (IOException exception) {
                throw new InvalidFeedException("Não consegui acessar esse feed.");
            }
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                String location = response.headers().firstValue("location").orElse("");
                if (location.isBlank()) {
                    throw new InvalidFeedException("O feed redirecionou sem destino.");
                }
                current = urls.parse(current.resolve(location).toString());
                continue;
            }
            if (status >= 400) {
                throw new InvalidFeedException("O feed respondeu com erro (" + status + ").");
            }
            byte[] raw = response.body() == null ? new byte[0] : response.body();
            if (raw.length > MAX_COMPRESSED) {
                throw new InvalidFeedException("Esse feed é grande demais.");
            }
            byte[] body = inflate(raw, response.headers().firstValue("content-encoding").orElse(""));
            if (body.length == 0) {
                throw new InvalidFeedException("O feed veio vazio.");
            }
            if (body.length > MAX_XML) {
                throw new InvalidFeedException("Esse feed é grande demais.");
            }
            return body;
        }
        throw new InvalidFeedException("Redirecionamentos demais nesse feed.");
    }

    private static byte[] inflate(byte[] raw, String encoding) {
        try {
            InputStream in;
            if (encoding.equalsIgnoreCase("gzip") || encoding.equalsIgnoreCase("x-gzip")) {
                in = new GZIPInputStream(new ByteArrayInputStream(raw));
            } else if (encoding.equalsIgnoreCase("deflate")) {
                in = new InflaterInputStream(new ByteArrayInputStream(raw));
            } else {
                return raw;
            }
            byte[] inflated = in.readNBytes(MAX_XML + 1);
            in.close();
            return inflated;
        } catch (IOException exception) {
            throw new InvalidFeedException("Não consegui ler esse feed.");
        }
    }
}
