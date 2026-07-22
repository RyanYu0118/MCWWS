/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.milkbowl.vault.economy.Economy
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryCloseEvent$Reason
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.PluginManager
 *  org.bukkit.plugin.RegisteredServiceProvider
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.scheduler.BukkitTask
 *  org.jetbrains.annotations.NotNull
 */
package cat.necko.bags;

import cat.necko.bags.bag.OpenBagListener;
import cat.necko.bags.bag.data.PlayerData;
import cat.necko.bags.bag.inventory.BagInventory;
import cat.necko.bags.bag.inventory.InventoryListener;
import cat.necko.bags.common.Command;
import cat.necko.bags.common.ServerListener;
import cat.necko.bags.config.Config;
import cat.necko.bags.config.Messages;
import cat.necko.bags.config.bags.BagsData;
import cat.necko.bags.config.bags.BagsFile;
import cat.necko.bags.config.items.ItemsData;
import cat.necko.bags.config.items.ItemsFile;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public final class Plugin
extends JavaPlugin {
    private static Plugin instance;
    private static Economy econ;
    private Config configData;
    private BagsFile bagsFile;
    private BagsData bagsData;
    private Messages messages;
    private ItemsFile itemsFile;
    private ItemsData itemsData;
    private ConcurrentMap<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<UUID, PlayerData>();
    private BukkitTask saveTask;
    private BukkitTask updateTask;

    public static Plugin getInstance() {
        return instance;
    }

    public void onEnable() {
        instance = this;
        this.setupConfigs();
        if (!this.setupEconomy()) {
            this.getLogger().severe(this.getMessages().getLegacyString("no-economy-plugin-found"));
            this.getServer().getPluginManager().disablePlugin((org.bukkit.plugin.Plugin)this);
            return;
        }
        this.playerDataMap = new ConcurrentHashMap<UUID, PlayerData>();
        Command command = new Command(this);
        Objects.requireNonNull(this.getCommand("bags")).setExecutor((CommandExecutor)command);
        Objects.requireNonNull(this.getCommand("bags")).setTabCompleter((TabCompleter)command);
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents((Listener)new OpenBagListener(this), (org.bukkit.plugin.Plugin)this);
        pm.registerEvents((Listener)new InventoryListener(this), (org.bukkit.plugin.Plugin)this);
        pm.registerEvents((Listener)new ServerListener(this), (org.bukkit.plugin.Plugin)this);
    }

    public void onDisable() {
        this.saveAll();
        this.closeAllBags();
    }

    private void setupConfigs() {
        this.bagsFile = new BagsFile(this);
        this.messages = new Messages(this);
        this.itemsFile = new ItemsFile(this);
        this.saveDefaultConfig();
        this.getConfig().options().copyDefaults(true);
    }

    private boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = (Economy)rsp.getProvider();
        return true;
    }

    public void reloadConfig() {
        long updateInterval;
        this.saveAll();
        this.closeAllBags();
        this.playerDataMap = new ConcurrentHashMap<UUID, PlayerData>();
        super.reloadConfig();
        this.bagsFile.reloadConfig();
        this.messages.reloadConfig();
        this.itemsFile.reloadConfig();
        this.configData = new Config(this.getConfig());
        this.bagsData = new BagsData(this.bagsFile);
        this.itemsData = new ItemsData(this.itemsFile);
        long saveInterval = this.getConfigData().saveInterval;
        if (saveInterval > 0L) {
            if (this.saveTask != null && !this.saveTask.isCancelled()) {
                this.saveTask.cancel();
            }
            this.saveTask = this.getServer().getScheduler().runTaskTimerAsynchronously((org.bukkit.plugin.Plugin)this, this::saveAll, saveInterval, saveInterval);
        }
        if ((updateInterval = this.getConfigData().updateInterval) > 0L) {
            if (this.updateTask != null && !this.updateTask.isCancelled()) {
                this.updateTask.cancel();
            }
            this.updateTask = this.getServer().getScheduler().runTaskTimerAsynchronously((org.bukkit.plugin.Plugin)this, () -> Bukkit.getOnlinePlayers().forEach(BagsData::updatePlayerBag), updateInterval, updateInterval);
        }
    }

    public Config getConfigData() {
        return this.configData;
    }

    public BagsData getBagsData() {
        return this.bagsData;
    }

    public Messages getMessages() {
        return this.messages;
    }

    public ItemsData getItemsData() {
        return this.itemsData;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public void closeAllBags() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (!BagInventory.isOpened(player)) {
                return;
            }
            player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
        });
    }

    public void saveAll() {
        this.playerDataMap.values().forEach(data -> {
            Player player = Bukkit.getPlayer((UUID)data.getUuid());
            String name = player == null ? data.getUuid().toString() : player.getName();
            try {
                data.save(this);
            }
            catch (IOException e) {
                this.getLogger().log(Level.SEVERE, "Could not save %s's player data".formatted(name), e);
            }
            if (player == null || !player.isOnline()) {
                this.playerDataMap.remove(data.getUuid());
            }
        });
    }

    @NotNull
    public PlayerData getPlayerData(@NotNull UUID uuid) {
        if (uuid == null) {
            Plugin.$$$reportNull$$$0(0);
        }
        if (!this.playerDataMap.containsKey(uuid)) {
            this.playerDataMap.put(uuid, new PlayerData(uuid));
        }
        PlayerData playerData = (PlayerData)this.playerDataMap.get(uuid);
        if (playerData == null) {
            Plugin.$$$reportNull$$$0(1);
        }
        return playerData;
    }

    private static /* synthetic */ void $$$reportNull$$$0(int n) {
        Object[] objectArray;
        Object[] objectArray2;
        Object[] objectArray3 = new Object[switch (n) {
            default -> 3;
            case 1 -> 2;
        }];
        switch (n) {
            default: {
                objectArray2 = objectArray3;
                objectArray3[0] = "uuid";
                break;
            }
            case 1: {
                objectArray2 = objectArray3;
                objectArray3[0] = "cat/necko/bags/Plugin";
                break;
            }
        }
        switch (n) {
            default: {
                objectArray = objectArray2;
                objectArray2[1] = "cat/necko/bags/Plugin";
                break;
            }
            case 1: {
                objectArray = objectArray2;
                objectArray2[1] = "getPlayerData";
                break;
            }
        }
        switch (n) {
            default: {
                objectArray = objectArray;
                objectArray[2] = "getPlayerData";
                break;
            }
            case 1: {
                break;
            }
        }
        String string = String.format(v0, objectArray);
        throw switch (n) {
            default -> new IllegalArgumentException(string);
            case 1 -> new IllegalStateException(string);
        };
    }
}

