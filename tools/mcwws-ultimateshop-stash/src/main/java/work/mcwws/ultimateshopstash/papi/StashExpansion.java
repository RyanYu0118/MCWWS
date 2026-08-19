package work.mcwws.ultimateshopstash.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.util.Messages;

public final class StashExpansion extends PlaceholderExpansion {

    private final McwwsUltimateShopStashPlugin plugin;

    public StashExpansion(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mcwws_stash";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MCWWS";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || params.isBlank()) {
            return "0";
        }
        String key = Messages.normalizeKey(params);
        return String.valueOf(plugin.storage().getAmount(player.getUniqueId(), key));
    }
}
