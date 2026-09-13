package work.mcwws.ultimateshopstash;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import work.mcwws.ultimateshopstash.catalog.ShopCatalog;
import work.mcwws.ultimateshopstash.collect.CollectListener;
import work.mcwws.ultimateshopstash.collect.ShopBuyDepositListener;
import work.mcwws.ultimateshopstash.command.StashCommand;
import work.mcwws.ultimateshopstash.exempt.ExemptManager;
import work.mcwws.ultimateshopstash.gui.WithdrawGuiListener;
import work.mcwws.ultimateshopstash.gui.WithdrawMenu;
import work.mcwws.ultimateshopstash.migrate.UltraDepositoryMigrator;
import work.mcwws.ultimateshopstash.papi.StashExpansion;
import work.mcwws.ultimateshopstash.returning.PendingReturnManager;
import work.mcwws.ultimateshopstash.shop.ShopDropListener;
import work.mcwws.ultimateshopstash.shop.ShopLorePatcher;
import work.mcwws.ultimateshopstash.shop.ShopReloadListener;
import work.mcwws.ultimateshopstash.storage.StashStorage;
import work.mcwws.ultimateshopstash.trade.TradeInterceptor;
import work.mcwws.ultimateshopstash.util.ChineseItemNames;
import work.mcwws.ultimateshopstash.util.Messages;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class McwwsUltimateShopStashPlugin extends JavaPlugin {

    private static McwwsUltimateShopStashPlugin instance;

    private Messages messages;
    private ShopCatalog catalog;
    private StashStorage storage;
    private ExemptManager exemptManager;
    private WithdrawMenu withdrawMenu;
    private ShopLorePatcher lorePatcher;
    private PendingReturnManager pendingReturns;
    /** 选块购买等：下一次商店买入不要把溢出吸进仓库，好让调用方把物品放到主手。 */
    private final Set<UUID> skipNextBuyDeposit = ConcurrentHashMap.newKeySet();

    public static McwwsUltimateShopStashPlugin getInstance() {
        return instance;
    }

    public Messages messages() {
        return messages;
    }

    public ShopCatalog catalog() {
        return catalog;
    }

    public StashStorage storage() {
        return storage;
    }

    public ExemptManager exemptManager() {
        return exemptManager;
    }

    public WithdrawMenu withdrawMenu() {
        return withdrawMenu;
    }

    public ShopLorePatcher lorePatcher() {
        return lorePatcher;
    }

    public PendingReturnManager pendingReturns() {
        return pendingReturns;
    }

    public String autoCollectPermission() {
        return getConfig().getString("auto-collect-permission", "ultradepository.*");
    }

    public int exemptDurationSeconds() {
        return getConfig().getInt("exempt-duration-seconds", 60);
    }

    public int maxWithdrawAmount() {
        return getConfig().getInt("max-withdraw-amount", 64);
    }

    public boolean collectPickup() {
        return getConfig().getBoolean("collect.pickup", true);
    }

    public boolean collectKill() {
        return getConfig().getBoolean("collect.kill", true);
    }

    public boolean depositShopBuys() {
        return getConfig().getBoolean("deposit-shop-buys", true);
    }

    /** 标记该玩家下一次商店买入跳过「溢出入库」。 */
    public void skipNextShopBuyDeposit(Player player) {
        if (player != null) {
            skipNextBuyDeposit.add(player.getUniqueId());
        }
    }

    /** @return true 表示本次应跳过入库（并已消费标记） */
    public boolean consumeSkipBuyDeposit(Player player) {
        return player != null && skipNextBuyDeposit.remove(player.getUniqueId());
    }

    @Override
    public void onEnable() {
        instance = this;
        if (getServer().getPluginManager().getPlugin("UltimateShop") == null) {
            getLogger().severe("未找到 UltimateShop，插件已禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        reloadLocal();

        getServer().getPluginManager().registerEvents(new CollectListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopBuyDepositListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopDropListener(this), this);
        getServer().getPluginManager().registerEvents(new WithdrawGuiListener(this), this);
        getServer().getPluginManager().registerEvents(lorePatcher, this);
        getServer().getPluginManager().registerEvents(new ShopReloadListener(this), this);
        getServer().getPluginManager().registerEvents(new TradeInterceptor(this), this);

        StashCommand command = new StashCommand(this);
        getCommand("mcwwsstash").setExecutor(command);
        getCommand("mcwwsstash").setTabCompleter(command);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new StashExpansion(this).register();
        }

        new UltraDepositoryMigrator(this).runIfNeeded();
        getLogger().info("UltimateShop 本地仓库已启用（替代 UltraDepository）。");
    }

    public void reloadLocal() {
        reloadConfig();
        ChineseItemNames.reload(this);
        messages = new Messages(this);
        catalog = new ShopCatalog(this);
        catalog.reload();
        storage = new StashStorage(this);
        exemptManager = new ExemptManager(this);
        withdrawMenu = new WithdrawMenu(this);
        lorePatcher = new ShopLorePatcher(this);
        pendingReturns = new PendingReturnManager(this);
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.saveAll();
        }
        instance = null;
    }

    public boolean shouldAutoCollect(org.bukkit.entity.Player player, String itemKey) {
        if (!hasAutoCollect(player)) {
            return false;
        }
        if (storage != null && storage.isSkipCollect(player.getUniqueId(), itemKey)) {
            return false;
        }
        return exemptManager == null || !exemptManager.isExempt(player, itemKey);
    }

    public boolean hasAutoCollect(org.bukkit.entity.Player player) {
        return player.hasPermission(autoCollectPermission());
    }
}
