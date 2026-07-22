/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Charsets
 *  org.bukkit.configuration.Configuration
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package cat.necko.bags.utils;

import cat.necko.bags.Plugin;
import com.google.common.base.Charsets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Level;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public abstract class AbstractConfig {
    public static String CONFIG_NAME;
    protected final Plugin plugin;
    protected final File configFile;
    protected FileConfiguration newConfig;

    protected AbstractConfig(Plugin plugin, String configName) {
        CONFIG_NAME = configName;
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), CONFIG_NAME);
        if (!this.configFile.exists()) {
            plugin.saveResource(CONFIG_NAME, false);
        }
        this.getConfig().options().copyDefaults(true);
    }

    public FileConfiguration getConfig() {
        if (this.newConfig == null) {
            this.reloadConfig();
        }
        return this.newConfig;
    }

    public void saveConfig() {
        try {
            this.getConfig().save(this.configFile);
        }
        catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not save config to " + String.valueOf(this.configFile), e);
        }
    }

    public void reloadConfig() {
        this.newConfig = YamlConfiguration.loadConfiguration((File)this.configFile);
        InputStream defConfigStream = this.plugin.getResource(CONFIG_NAME);
        if (defConfigStream != null) {
            this.newConfig.setDefaults((Configuration)YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(defConfigStream, Charsets.UTF_8)));
        }
    }
}

