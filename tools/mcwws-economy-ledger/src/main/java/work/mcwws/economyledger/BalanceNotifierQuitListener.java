package work.mcwws.economyledger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

final class BalanceNotifierQuitListener implements Listener {

    private final BalanceNotifier notifier;

    BalanceNotifierQuitListener(BalanceNotifier notifier) {
        this.notifier = notifier;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        notifier.clear(event.getPlayer());
    }
}
