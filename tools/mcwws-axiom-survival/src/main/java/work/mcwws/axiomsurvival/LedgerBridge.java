package work.mcwws.axiomsurvival;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

final class LedgerBridge {

    private static volatile Boolean available;

    private LedgerBridge() {
    }

    static boolean withdraw(Player player, double amount, String label) {
        if (player == null || amount <= 0D) {
            return true;
        }
        if (isLedgerAvailable()) {
            String refId = "axiom-" + java.util.UUID.randomUUID();
            String description = "Axiom 建造: " + label;
            boolean[] ok = {false};
            Runnable action = () -> ok[0] = EconomyService.withdraw(player, amount);
            if (runWithLedger(player, "axiom", description, refId, action)) {
                return ok[0];
            }
        }
        return EconomyService.withdraw(player, amount);
    }

    static boolean deposit(Player player, double amount, String description, String refId) {
        if (player == null || amount <= 0D) {
            return true;
        }
        if (isLedgerAvailable()) {
            boolean[] ok = {false};
            Runnable action = () -> ok[0] = EconomyService.deposit(player, amount);
            if (runWithLedger(player, "axiom_undo", description, refId, action)) {
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
            Runnable action
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
            McwwsAxiomSurvivalPlugin.getInstance().getLogger().fine("LedgerContext 调用失败，退回普通经济操作。");
            return false;
        }
    }

    private static boolean isLedgerAvailable() {
        if (available != null) {
            return available;
        }
        McwwsAxiomSurvivalPlugin plugin = McwwsAxiomSurvivalPlugin.getInstance();
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
