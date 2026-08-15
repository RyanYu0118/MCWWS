package work.mcwws.residencequiet;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 玩家停留在同一领地期间，同类拒绝提示只放行一次；换领地（含进出荒野）后重置。
 */
final class DenyThrottle {

    private final boolean enabled;
    private final List<Pattern> patterns;
    /** player -> 本趟已显示过的纯文本指纹 */
    private final Map<UUID, Set<String>> shown = new ConcurrentHashMap<>();

    private DenyThrottle(boolean enabled, List<Pattern> patterns) {
        this.enabled = enabled;
        this.patterns = List.copyOf(patterns);
    }

    static DenyThrottle fromConfig(FileConfiguration config) {
        boolean enabled = config.getBoolean("enabled", true);
        List<Pattern> patterns = new ArrayList<>();
        for (String raw : config.getStringList("patterns")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            patterns.add(Pattern.compile(raw.trim()));
        }
        if (patterns.isEmpty()) {
            patterns.add(Pattern.compile("你没有 .+ 权限\\.?"));
            patterns.add(Pattern.compile("你没有领地 .+ 的 .+ 权限\\.?"));
            patterns.add(Pattern.compile("你没有领地 .+ 的移动权限\\.?"));
        }
        return new DenyThrottle(enabled, patterns);
    }

    boolean enabled() {
        return enabled;
    }

    void resetVisit(UUID playerId) {
        if (playerId != null) {
            shown.remove(playerId);
        }
    }

    void clearAll() {
        shown.clear();
    }

    /**
     * @return true 表示这条消息应当发给玩家；false 表示本趟已提示过，应拦截
     */
    boolean allow(UUID playerId, String plainText) {
        if (!enabled || playerId == null || plainText == null || plainText.isBlank()) {
            return true;
        }
        String normalized = normalize(plainText);
        if (!isDenyTip(normalized)) {
            return true;
        }
        Set<String> bag = shown.computeIfAbsent(playerId, id -> ConcurrentHashMap.newKeySet());
        return bag.add(normalized);
    }

    private boolean isDenyTip(String normalized) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(normalized).matches()) {
                return true;
            }
        }
        return false;
    }

    static String normalize(String text) {
        String stripped = text
                .replaceAll("§[0-9A-FK-ORa-fk-orx]", "")
                .replaceAll("&[0-9A-FK-ORa-fk-orx]", "")
                .replace('\u00A0', ' ')
                .trim();
        return stripped.replaceAll("\\s+", " ");
    }
}
