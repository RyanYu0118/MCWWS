package work.mcwws.worldedit;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.DecimalFormat;

public final class EconomyService {

    private static Economy economy;
    private static final DecimalFormat MONEY = new DecimalFormat("#0.##");

    private EconomyService() {
    }

    public static boolean hook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static double getBalance(Player player) {
        if (economy == null || player == null) {
            return 0D;
        }
        return economy.getBalance(player);
    }

    public static boolean withdraw(Player player, double amount) {
        if (economy == null || player == null || amount <= 0D) {
            return true;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public static String format(double amount) {
        return MONEY.format(amount);
    }
}
