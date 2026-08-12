package work.mcwws.worldedit;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

final class LedgerBridge {

    private static volatile Boolean available;

    private LedgerBridge() {
    }

    static boolean withdraw(Player player, double amount, String command) {
        return withdraw(player, amount, "worldedit", "创世神建造: " + command, "we-" + UUID.randomUUID());
    }

    static boolean withdraw(Player player, double amount, String category, String description, String refId) {
        if (player == null || amount <= 0D) {
            return true;
        }
        if (isLedgerAvailable()) {
            boolean[] ok = {false};
            Runnable action = () -> ok[0] = EconomyService.withdraw(player, amount);
            if (runWithLedger(player, category, description, refId, action, "扣款")) {
                return ok[0];
            }
        }
        return EconomyService.withdraw(player, amount);
    }

    static boolean deposit(Player player, double amount, String category, String description, String refId) {
        if (player == null || amount <= 0D) {
            return true;
        }
        if (isLedgerAvailable()) {
            boolean[] ok = {false};
            Runnable action = () -> ok[0] = EconomyService.deposit(player, amount);
            if (runWithLedger(player, category, description, refId, action, "入账")) {
                return ok[0];
            }
        }
        return EconomyService.deposit(player, amount);
    }

    private static boolean runWithLedger(
            Player player,
            String category,
            String description,
            String refId,
            Runnable action,
            String what
    ) {
        try {
            Class<?> contextClass = Class.forName("work.mcwws.economyledger.LedgerContext");
            Method runWith = contextClass.getMethod(
                    "runWith",
                    Player.class,
                    String.class,
                    String.class,
                    String.class,
                    Runnable.class
            );
            runWith.invoke(null, player, category, description, refId, action);
            return true;
        } catch (ReflectiveOperationException ex) {
            McwwsWeSurvivalPlugin.getInstance().getLogger()
                    .fine("LedgerContext " + what + "调用失败，退回普通经济操作。");
            return false;
        }
    }

    private static boolean isLedgerAvailable() {
        if (available != null) {
            return available;
        }
        McwwsWeSurvivalPlugin plugin = McwwsWeSurvivalPlugin.getInstance();
        if (plugin == null) {
            return false;
        }
        try {
            Class.forName("work.mcwws.economyledger.LedgerContext");
            available = plugin.getServer().getPluginManager().getPlugin("MCWWS_EconomyLedger") != null;
        } catch (ClassNotFoundException ex) {
            available = false;
        }
        return available;
    }
}
