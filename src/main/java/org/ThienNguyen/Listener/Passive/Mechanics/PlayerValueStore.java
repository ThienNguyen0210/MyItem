package org.ThienNguyen.Listener.Passive.Mechanics;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class PlayerValueStore {

    private PlayerValueStore() {}

    private static final class Entry {
        final String value;
        final long expireAt; // Long.MAX_VALUE == never expires

        Entry(String value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        boolean isExpired() {
            return expireAt != Long.MAX_VALUE && System.currentTimeMillis() >= expireAt;
        }
    }

    private static final Map<UUID, Map<String, Entry>> STORE = new ConcurrentHashMap<>();


    public static void set(UUID uuid, String key, String value, long durationMillis) {
        if (uuid == null || key == null || key.isEmpty()) return;
        long expireAt = durationMillis > 0 ? System.currentTimeMillis() + durationMillis : Long.MAX_VALUE;
        STORE.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(key, new Entry(value, expireAt));
    }


    public static String get(UUID uuid, String key) {
        if (uuid == null || key == null) return null;
        Map<String, Entry> map = STORE.get(uuid);
        if (map == null) return null;

        Entry e = map.get(key);
        if (e == null) return null;

        if (e.isExpired()) {
            map.remove(key);
            return null;
        }
        return e.value;
    }


    public static boolean has(UUID uuid, String key) {
        return get(uuid, key) != null;
    }


    public static void remove(UUID uuid, String key) {
        if (uuid == null || key == null) return;
        Map<String, Entry> map = STORE.get(uuid);
        if (map != null) map.remove(key);
    }


    public static void clear(UUID uuid) {
        if (uuid == null) return;
        STORE.remove(uuid);
    }
}