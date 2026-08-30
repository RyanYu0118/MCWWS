package work.mcwws.immersivecreative;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.DecimalFormat;

public final class EconomyHook {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");
    private net.milkbowl.vault.economy.Economy economy;

    public boolean hook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public double getBalance(Player player) {
        return economy == null || player == null ? 0D : economy.getBalance(player);
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null || player == null || amount <= 0D) {
            return true;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null || player == null || amount <= 0D) {
            return true;
        }
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public static String format(double amount) {
        return MONEY.format(amount);
    }
}
