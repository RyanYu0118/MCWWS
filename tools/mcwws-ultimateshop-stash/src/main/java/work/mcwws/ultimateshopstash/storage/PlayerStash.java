package work.mcwws.ultimateshopstash.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class PlayerStash {

    private final Map<String, Long> amounts = new HashMap<>();
    private final Set<String> skipCollect = new HashSet<>();

    long getAmount(String itemKey) {
        return amounts.getOrDefault(itemKey, 0L);
    }

    long add(String itemKey, long delta) {
        long next = getAmount(itemKey) + delta;
        amounts.put(itemKey, next);
        return next;
    }

    long remove(String itemKey, long delta) {
        long current = getAmount(itemKey);
        long next = Math.max(0L, current - delta);
        if (next <= 0) {
            amounts.remove(itemKey);
        } else {
            amounts.put(itemKey, next);
        }
        return next;
    }

    boolean tryRemove(String itemKey, long delta) {
        long current = getAmount(itemKey);
        if (current < delta) {
            return false;
        }
        remove(itemKey, delta);
        return true;
    }

    void put(String itemKey, long amount) {
        if (amount <= 0) {
            amounts.remove(itemKey);
        } else {
            amounts.put(itemKey, amount);
        }
    }

    Map<String, Long> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(amounts));
    }

    boolean isSkipCollect(String itemKey) {
        return skipCollect.contains(itemKey);
    }

    boolean toggleSkipCollect(String itemKey) {
        if (skipCollect.remove(itemKey)) {
            return false;
        }
        skipCollect.add(itemKey);
        return true;
    }

    void setSkipCollect(String itemKey, boolean skip) {
        if (skip) {
            skipCollect.add(itemKey);
        } else {
            skipCollect.remove(itemKey);
        }
    }

    Set<String> skipCollectSnapshot() {
        return Collections.unmodifiableSet(new HashSet<>(skipCollect));
    }
}
