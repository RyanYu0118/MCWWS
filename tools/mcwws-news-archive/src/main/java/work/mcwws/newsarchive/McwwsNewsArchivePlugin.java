package work.mcwws.newsarchive;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class McwwsNewsArchivePlugin extends JavaPlugin {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private ArchiveStore store;
    private BookRenderer bookRenderer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new ArchiveStore(this);
        bookRenderer = new BookRenderer(store);
        store.reload();
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

    public void reloadAll() {
        reloadConfig();
        store.reload();
        syncFromBookNewsQuiet();
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
