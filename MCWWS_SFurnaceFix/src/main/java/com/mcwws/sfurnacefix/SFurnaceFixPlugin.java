package com.mcwws.sfurnacefix;

import org.bukkit.plugin.java.JavaPlugin;

public final class SFurnaceFixPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new FurnaceMenuListener(this), this);
        getLogger().info("[MCWWS] Slimefun furnace BlockMenu fix enabled.");
    }
}
