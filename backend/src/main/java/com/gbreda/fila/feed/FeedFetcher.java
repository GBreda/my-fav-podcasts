package com.gbreda.fila.feed;

import java.net.URI;

public interface FeedFetcher {

    byte[] fetch(URI uri);
}
