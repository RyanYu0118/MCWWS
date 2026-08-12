package work.mcwws.economyledger;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

final class EssentialsBalanceListener implements Listener {

    private static final String EVENT_CLASS = "net.ess3.api.events.UserBalanceUpdateEvent";

    private final McwwsEconomyLedgerPlugin plugin;
    private final LedgerQueueWriter writer;
    private final DedupCache dedupCache;
    private volatile boolean essentialsEventAvailable;

    EssentialsBalanceListener(McwwsEconomyLedgerPlugin plugin, LedgerQueueWriter writer, DedupCache dedupCache) {
        this.plugin = plugin;
        this.writer = writer;
        this.dedupCache = dedupCache;
    }

    void register() {
        try {
            Class.forName(EVENT_CLASS);
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(EVENT_CLASS);
            PluginManager manager = plugin.getServer().getPluginManager();
            EventExecutor executor = (listener, event) -> {
                try {
                    handleBalanceUpdate(event);
                } catch (ReflectiveOperationException ex) {
                    plugin.getLogger().log(Level.WARNING, "处理 Essentials 余额事件失败", ex);
                } finally {
                    LedgerContext.clear();
                }
            };
            manager.registerEvent(eventClass, this, EventPriority.MONITOR, executor, plugin, true);
            essentialsEventAvailable = true;
            plugin.getLogger().info("已注册 Essentials UserBalanceUpdateEvent 监听。");
        } catch (ClassNotFoundException ex) {
            essentialsEventAvailable = false;
            plugin.getLogger().warning("未找到 Essentials 余额事件类，经济层兜底未启用。");
        }
    }

    void probeEssentialsEvent() {
        essentialsEventAvailable = classExists(EVENT_CLASS);
        if (!essentialsEventAvailable) {
            plugin.getLogger().warning("未找到 Essentials 余额事件类。");
        }
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private void handleBalanceUpdate(Event event) throws ReflectiveOperationException {
        Method getPlayer = event.getClass().getMethod("getPlayer");
        Method getOldBalance = event.getClass().getMethod("getOldBalance");
        Method getNewBalance = event.getClass().getMethod("getNewBalance");
        Method getCause = event.getClass().getMethod("getCause");

        Player player = (Player) getPlayer.invoke(event);
        if (player == null) {
            return;
        }

        BigDecimal oldBalance = (BigDecimal) getOldBalance.invoke(event);
        BigDecimal newBalance = (BigDecimal) getNewBalance.invoke(event);
        if (oldBalance == null || newBalance == null) {
            return;
        }

        double oldValue = oldBalance.doubleValue();
        double newValue = newBalance.doubleValue();
        double delta = Math.round((newValue - oldValue) * 100.0) / 100.0;
        if (Math.abs(delta) < 0.0001D) {
            return;
        }

        Enum<?> causeEnum = (Enum<?>) getCause.invoke(event);
        String cause = causeEnum == null ? "UNKNOWN" : causeEnum.name();

        String direction = LedgerQueueWriter.directionForDelta(delta);
        double amount = LedgerQueueWriter.absAmount(delta);

        UUID uuid = player.getUniqueId();
        Optional<LedgerContext.Entry> context = LedgerContext.peek(uuid);

        String category = context.map(LedgerContext.Entry::category)
                .filter(value -> !value.isBlank())
                .orElseGet(() -> plugin.categoryForCause(cause));
        String description = context.map(LedgerContext.Entry::description)
                .filter(value -> !value.isBlank())
                .orElseGet(() -> plugin.descriptionForCause(cause, direction));
        String refId = context.map(LedgerContext.Entry::refId)
                .filter(value -> !value.isBlank())
                .orElseGet(() -> LedgerQueueWriter.defaultRefId("ess", uuid.toString(), amount, cause));

        boolean flightLike = shouldSkipFlight(direction, amount, cause);
        // 屏幕提示要覆盖全部余额变动，包括飞行这种账本里另有来源、以及 refId 相同会被去重的变动
        BalanceNotifier notifier = plugin.getBalanceNotifier();
        if (notifier != null) {
            notifier.notifyChange(
                    player,
                    delta,
                    newValue,
                    flightLike ? plugin.getFlightCategory() : category,
                    flightLike ? plugin.getFlightDescription() : description
            );
        }

        if (flightLike) {
            return;
        }

        if (dedupCache.shouldSkip("ref:" + refId)) {
            return;
        }

        writer.append(
                uuid.toString(),
                player.getName(),
                direction,
                category,
                amount,
                newValue,
                description,
                refId
        );
    }

    private boolean shouldSkipFlight(String direction, double amount, String cause) {
        if (!"debit".equals(direction) || !plugin.isFlightSkipEnabled()) {
            return false;
        }
        if (!"API".equals(cause) && !"UNKNOWN".equals(cause)) {
            return false;
        }
        List<Double> amounts = plugin.getFlightAmounts();
        for (double flightAmount : amounts) {
            if (Math.abs(amount - flightAmount) < 0.011D) {
                return true;
            }
        }
        return false;
    }
}
