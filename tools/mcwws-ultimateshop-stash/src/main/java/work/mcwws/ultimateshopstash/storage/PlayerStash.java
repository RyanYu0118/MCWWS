package work.mcwws.ultimateshopstash.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class PlayerStash {

    private final Map<String, Long> amounts = new HashMap<>();

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
}
