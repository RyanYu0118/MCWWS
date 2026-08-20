package work.mcwws.ideachievements;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import me.pulsi_.bankplus.economy.TransactionType;
import me.pulsi_.bankplus.events.BPAfterTransactionEvent;

public final class BankPlusMoneyListener implements Listener {

    private static final Set<TransactionType> CREDIT = EnumSet.of(
            TransactionType.ADD,
            TransactionType.DEPOSIT,
            TransactionType.INTEREST);

    private static final Set<TransactionType> DEBIT = EnumSet.of(
            TransactionType.REMOVE,
            TransactionType.WITHDRAW,
            TransactionType.PAY,
            TransactionType.LOAN);

    private final McwwsIdeaAchievementsPlugin plugin;

    public BankPlusMoneyListener(McwwsIdeaAchievementsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBankTx(BPAfterTransactionEvent event) {
        OfflinePlayer off = event.getPlayer();
        if (off == null) {
            return;
        }
        Player online = off.getPlayer();
        if (online == null || !online.isOnline()) {
            return;
        }
        double amount = event.getTransactionAmount() == null
                ? 0
                : event.getTransactionAmount().doubleValue();
        if (amount <= 0) {
            return;
        }
        TransactionType type = event.getTransactionType();
        if (type == null || type == TransactionType.SET) {
            return;
        }
        boolean credit = CREDIT.contains(type);
        boolean debit = DEBIT.contains(type);
        if (!credit && !debit) {
            plugin.economyFlow().onMoneyFlow(online.getUniqueId(), amount, true);
            return;
        }
        plugin.economyFlow().onMoneyFlow(online.getUniqueId(), amount, credit);
    }
}
