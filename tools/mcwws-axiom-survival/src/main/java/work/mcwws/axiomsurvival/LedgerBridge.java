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
                runWith.invoke(null, player, "axiom", description, refId, action);
                return ok[0];
            } catch (ReflectiveOperationException ex) {
                McwwsAxiomSurvivalPlugin.getInstance().getLogger().fine("LedgerContext 调用失败，退回普通扣款。");
            }
        }
        return EconomyService.withdraw(player, amount);
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
