package work.mcwws.immersivecreative;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ImmersivePlaceholders extends PlaceholderExpansion {

    private final McwwsImmersiveCreativePlugin plugin;

    public ImmersivePlaceholders(McwwsImmersiveCreativePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mcwwsic";
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
        if (!"enabled".equalsIgnoreCase(params)) {
            return "";
        }
        Player online = player instanceof Player p ? p : player.getPlayer();
        if (online == null) {
            return "no";
        }
        return plugin.state().isEnabled(online) ? "yes" : "no";
    }
}
