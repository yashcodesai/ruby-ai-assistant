package com.ruby.ruby.integration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Remembers online lookups for a while so repeated questions answer
 * instantly and we do not hammer free public APIs.
 */
class TtlCache<K, V> {

    private static final int MAX_ENTRIES = 200;

    private record Entry<V>(V value, Instant expires) {
    }

    private final Map<K, Entry<V>> entries = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    TtlCache(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    V get(K key, Function<K, V> loader) {
        Instant now = clock.instant();
        Entry<V> cached = entries.get(key);
        if (cached != null && cached.expires().isAfter(now)) {
            return cached.value();
        }
        V value = loader.apply(key);
        if (entries.size() >= MAX_ENTRIES) {
            entries.clear();
        }
        entries.put(key, new Entry<>(value, now.plus(ttl)));
        return value;
    }
}
