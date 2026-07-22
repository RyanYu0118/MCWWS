/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 */
package cat.necko.bags.config;

import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    public final boolean autoPicking;
    public final boolean pickupOnlySellable;
    public final boolean playerPutAbility;
    public final boolean playerGetAbility;
    public final boolean bagMoveAbility;
    public final int bagSlot;
    public final boolean sellAll;
    public final long saveInterval;
    public final long updateInterval;
    public final boolean shouldGiveOnRespawn;
    public final boolean shouldGiveOnJoin;
    public final String bagInventoryLabel;

    public Config(FileConfiguration config) {
        this.autoPicking = config.getBoolean("auto-picking", true);
        this.pickupOnlySellable = config.getBoolean("pickup-only-sellable", false);
        this.playerPutAbility = config.getBoolean("player-put-ability", true);
        this.playerGetAbility = config.getBoolean("player-get-ability", true);
        this.bagMoveAbility = config.getBoolean("bag-move-ability", false);
        this.bagSlot = config.getInt("bag-slot", 4);
        this.sellAll = config.getBoolean("sell-all", true);
        this.saveInterval = config.getLong("save-interval", 600L) * 20L;
        this.updateInterval = config.getLong("update-interval", -1L) * 20L;
        this.shouldGiveOnRespawn = config.getBoolean("should-give-bag-on-respawn", true);
        this.shouldGiveOnJoin = config.getBoolean("should-give-bag-on-join", true);
        this.bagInventoryLabel = config.getString("bag-inventory-label", "\u0421\u0443\u043c\u043a\u0430 \u0438\u0433\u0440\u043e\u043a\u0430 %player% - %current-level% \u0443\u0440. (%items-sum% \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432)");
    }
}

