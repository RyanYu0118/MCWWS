package work.mcwws.worldsync;

/**
 * Console progress for a long flush or pull. Safe to read from the login thread.
 */
public final class SyncProgress {
    private volatile String phase = "";
    private volatile String current = "";
    private volatile int done;
    private volatile int total;
    private volatile boolean active;
    private long lastLog;

    public synchronized void begin(String phase, int total) {
        this.phase = phase == null ? "" : phase;
        this.total = Math.max(0, total);
        this.done = 0;
        this.current = "";
        this.active = this.total > 0;
        this.lastLog = 0;
    }

    public synchronized void tick(McwwsWorldSyncPlugin plugin, int doneNow, String path) {
        done = doneNow;
        current = path == null ? "" : path;
        active = true;
        long now = System.currentTimeMillis();
        int pct = percent();
        int prev = total <= 0 ? 0 : Math.max(0, doneNow - 1) * 100 / total;
        boolean edge = doneNow <= 1 || doneNow >= total;
        if (edge || now - lastLog >= 2000L || (pct != prev && pct % 5 == 0)) {
            lastLog = now;
            plugin.getLogger().info(consoleLine());
        }
    }

    public synchronized void end(McwwsWorldSyncPlugin plugin) {
        if (active && total > 0 && done < total) {
            plugin.getLogger().info(consoleLine());
        }
        active = false;
        current = "";
    }

    public boolean active() {
        return active && total > 0;
    }

    public String consoleLine() {
        int pct = percent();
        String pctText = pctText();
        String path = current.isEmpty() ? "" : "  " + shorten(current);
        return phase + " " + bar(pct) + " " + pctText + "  " + Math.min(done, total) + "/" + total + path;
    }

    /** Shown on the disconnect screen while a transfer is running. */
    public String kickHint() {
        if (!active()) {
            return "正在从另一端同步最新世界，请稍后重新连接。";
        }
        int pct = percent();
        return "正在从另一端同步最新世界（" + phase + " " + bar(pct) + " " + pctText() + "，"
                + Math.min(done, total) + "/" + total + "），请稍后重新连接。";
    }

    private int percent() {
        if (total <= 0) {
            return 0;
        }
        return Math.min(100, done * 100 / total);
    }

    private String pctText() {
        if (total <= 0) {
            return "0%";
        }
        int pct = percent();
        if (pct == 0 && done > 0) {
            return String.format(java.util.Locale.ROOT, "%.1f%%", done * 100.0 / total);
        }
        return pct + "%";
    }

    static String bar(int pct) {
        int filled = Math.max(0, Math.min(10, pct / 10));
        return "[" + "#".repeat(filled) + "-".repeat(10 - filled) + "]";
    }

    static String shorten(String path) {
        if (path.length() <= 72) {
            return path;
        }
        return "…" + path.substring(path.length() - 71);
    }
}
