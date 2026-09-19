package work.mcwws.newsarchive;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class McwwsNewsArchivePlugin extends JavaPlugin {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private ArchiveStore store;
    private BookRenderer bookRenderer;
    private JoinPrefs joinPrefs;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new ArchiveStore(this);
        bookRenderer = new BookRenderer(store);
        joinPrefs = new JoinPrefs(this);
        store.reload();
        joinPrefs.reload();
        syncFromBookNewsQuiet();

        ArchiveCommand command = new ArchiveCommand(this);
        var pluginCommand = getCommand("newsarchive");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getServer().getPluginManager().registerEvents(new ArchiveListener(this), this);
        getLogger().info("MCWWS_NewsArchive 已启用，当前留档 " + store.listNewestFirst().size() + " 期。");
    }

    public ArchiveStore store() {
        return store;
    }

    public BookRenderer bookRenderer() {
        return bookRenderer;
    }

    public JoinPrefs joinPrefs() {
        return joinPrefs;
    }

    public void reloadAll() {
        reloadConfig();
        store.reload();
        joinPrefs.reload();
        getServer().getOnlinePlayers().forEach(joinPrefs::applyPermission);
        syncFromBookNewsQuiet();
    }

    public boolean shouldOpenOnJoin(Player player) {
        if (player == null || player.hasPermission("booknews.skip")) {
            return false;
        }
        NewsVersion latest = store.latest();
        if (latest == null) {
            return false;
        }
        if (joinPrefs.isAlways(player.getUniqueId())) {
            return true;
        }
        return !store.hasRead(player.getUniqueId(), latest.id());
    }

    public void openLatestForJoin(Player player) {
        NewsVersion latest = store.latest();
        if (latest == null || player == null || !player.isOnline()) {
            return;
        }
        store.markRead(player.getUniqueId(), latest.id());
        playOpenSound(player);
        bookRenderer.open(player, latest);
    }

    public long joinOpenDelayTicks() {
        long configuredDelay = getConfig().getLong("join.delay-seconds", -1L);
        if (configuredDelay >= 0L) {
            return configuredDelay * 20L;
        }
        long fromBookNews = 5L;
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(
                    resolveServerFile(getConfig().getString("booknews-config", "plugins/BookNews/config.yml")));
            fromBookNews = Math.max(0L, cfg.getLong("OpenBookDelaySecond", 5L));
        } catch (Exception ignored) {
            // keep default
        }
        return fromBookNews * 20L;
    }

    private void playOpenSound(Player player) {
        String name = getConfig().getString("join.open-sound", "ENTITY_PLAYER_LEVELUP");
        if (name == null || name.isBlank() || name.equalsIgnoreCase("none")) {
            return;
        }
        try {
            player.playSound(player.getLocation(), Sound.valueOf(name.toUpperCase()), 1f, 1f);
        } catch (IllegalArgumentException ignored) {
            // invalid sound name
        }
    }

    public void syncFromBookNewsQuiet() {
        NewsVersion created = store.syncFromBookNews();
        if (created != null) {
            getLogger().info("已自动留档新版本: " + created.id() + " · " + created.title());
        }
    }

    public File resolveServerFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return new File(".");
        }
        File direct = new File(relativePath);
        if (direct.isAbsolute()) {
            return direct;
        }
        return new File(getDataFolder().getParentFile().getParentFile(), relativePath);
    }

    public void send(CommandSender sender, String path, String... replacements) {
        String prefix = getConfig().getString("messages.prefix", "");
        String raw = getConfig().getString(path, path);
        if (replacements != null) {
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        sender.sendMessage(LEGACY.deserialize(prefix + raw));
    }
}
