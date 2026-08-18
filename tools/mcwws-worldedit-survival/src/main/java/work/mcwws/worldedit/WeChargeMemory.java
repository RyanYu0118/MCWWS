package work.mcwws.worldedit;

import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每位玩家的创世神扣费历史，与 FAWE 的 undo/redo 栈对齐：
 * 新操作入 done、清空 redo；undo 从 done 弹出并压入 undone；redo 相反。
 */
final class WeChargeMemory {

    record Entry(double grossAmount, String command, List<String> marketLines) {
    }

    private static final class Stacks {
        final ArrayDeque<Entry> done = new ArrayDeque<>();
        final ArrayDeque<Entry> undone = new ArrayDeque<>();
    }

    private static final ConcurrentHashMap<UUID, Stacks> STACKS = new ConcurrentHashMap<>();

    private WeChargeMemory() {
    }

    /** grossAmount 为带符号净额：正数是实扣，负数是拆除回收后的净到账 */
    static void record(Player player, double grossAmount, String command, List<String> marketLines) {
        if (player == null) {
            return;
        }
        List<String> lines = marketLines == null ? List.of() : List.copyOf(marketLines);
        if (Math.abs(grossAmount) < 0.01D && lines.isEmpty()) {
            return;
        }
        String cmd = command == null ? "worldedit" : command;
        Entry entry = new Entry(grossAmount, cmd, lines);
        Stacks stacks = STACKS.computeIfAbsent(player.getUniqueId(), key -> new Stacks());
        synchronized (stacks) {
            stacks.done.addLast(entry);
            stacks.undone.clear();
            int max = maxSize();
            while (stacks.done.size() > max) {
                stacks.done.removeFirst();
            }
        }
    }

    static List<Entry> takeDone(Player player, int times) {
        return take(player, times, true);
    }

    static List<Entry> takeUndone(Player player, int times) {
        return take(player, times, false);
    }

    static void restoreDone(Player player, List<Entry> taken) {
        restore(player, taken, true);
    }

    static void restoreUndone(Player player, List<Entry> taken) {
        restore(player, taken, false);
    }

    static void pushUndone(Player player, List<Entry> taken) {
        push(player, taken, false);
    }

    static void pushDone(Player player, List<Entry> taken) {
        push(player, taken, true);
    }

    static int maxSize() {
        McwwsWeSurvivalPlugin plugin = McwwsWeSurvivalPlugin.getInstance();
        int configured = plugin == null ? 128 : plugin.getPluginConfig().getInt("undo-refund.history-size", 128);
        return Math.max(configured, 1);
    }

    private static List<Entry> take(Player player, int times, boolean fromDone) {
        if (player == null || times <= 0) {
            return List.of();
        }
        Stacks stacks = STACKS.get(player.getUniqueId());
        if (stacks == null) {
            return List.of();
        }
        synchronized (stacks) {
            ArrayDeque<Entry> source = fromDone ? stacks.done : stacks.undone;
            List<Entry> out = new ArrayList<>();
            for (int i = 0; i < times && !source.isEmpty(); i++) {
                out.add(source.removeLast());
            }
            return out;
        }
    }

    private static void restore(Player player, List<Entry> taken, boolean toDone) {
        if (player == null || taken == null || taken.isEmpty()) {
            return;
        }
        Stacks stacks = STACKS.computeIfAbsent(player.getUniqueId(), key -> new Stacks());
        synchronized (stacks) {
            ArrayDeque<Entry> target = toDone ? stacks.done : stacks.undone;
            for (int i = taken.size() - 1; i >= 0; i--) {
                target.addLast(taken.get(i));
            }
        }
    }

    private static void push(Player player, List<Entry> taken, boolean toDone) {
        if (player == null || taken == null || taken.isEmpty()) {
            return;
        }
        Stacks stacks = STACKS.computeIfAbsent(player.getUniqueId(), key -> new Stacks());
        synchronized (stacks) {
            ArrayDeque<Entry> target = toDone ? stacks.done : stacks.undone;
            for (Entry entry : taken) {
                target.addLast(entry);
            }
            int max = maxSize();
            while (target.size() > max) {
                target.removeFirst();
            }
        }
    }
}
