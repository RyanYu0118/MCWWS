package work.mcwws.economyledger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class DedupCache {

    private final Map<String, Long> recent = new ConcurrentHashMap<>();
    private final long windowMs;

    DedupCache(long windowMs) {
        this.windowMs = Math.max(windowMs, 500L);
    }

    boolean shouldSkip(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long previous = recent.put(key, now);
        purgeExpired(now);
        return previous != null && now - previous < windowMs;
    }

    private void purgeExpired(long now) {
        if (recent.size() < 256) {
            return;
        }
        Iterator<Map.Entry<String, Long>> iterator = recent.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > windowMs * 4L) {
                iterator.remove();
            }
        }
    }
}
