package work.mcwws.webhost;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class McwwsWebHostPlugin extends JavaPlugin {

    private static McwwsWebHostPlugin instance;
    private WebProcessManager webProcess;
    private FileConfiguration pluginConfig;

    public static McwwsWebHostPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadLocalConfig();
        webProcess = new WebProcessManager(this);

        long delay = Math.max(getPluginConfig().getLong("startup-delay-ticks", 60L), 0L);
        if (delay <= 0L) {
            webProcess.start();
        } else {
            getServer().getScheduler().runTaskLater(this, () -> webProcess.start(), delay);
        }
    }

    @Override
    public void onDisable() {
        if (webProcess != null) {
            webProcess.stop();
        }
        instance = null;
    }

    public void reloadLocalConfig() {
        reloadConfig();
        pluginConfig = getConfig();
    }

    public FileConfiguration getPluginConfig() {
        return pluginConfig;
    }

    public File getServerRoot() {
        File worldContainer = getServer().getWorldContainer();
        if (worldContainer != null) {
            File parent = worldContainer.getParentFile();
            if (parent != null) {
                return parent;
            }
        }
        File pluginsDir = getDataFolder().getParentFile();
        if (pluginsDir != null && "plugins".equalsIgnoreCase(pluginsDir.getName())) {
            File root = pluginsDir.getParentFile();
            if (root != null) {
                return root;
            }
        }
        return new File(".").getAbsoluteFile();
    }

    public File resolveWebDirectory() {
        String relative = getPluginConfig().getString("web-directory", "plugins/Skript/scripts/web");
        return new File(getServerRoot(), relative.replace('/', File.separatorChar));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (webProcess == null) {
            sender.sendMessage("§c[MCWWS] WebHost 未初始化。");
            return true;
        }
        String name = command.getName().toLowerCase();
        if ("mcwws-web-reload".equals(name)) {
            reloadLocalConfig();
            boolean ok = webProcess.restart();
            sender.sendMessage(ok
                    ? "§a[MCWWS] 网页 Node 已重启。"
                    : "§c[MCWWS] 网页 Node 重启失败，请查看控制台。");
            return true;
        }
        if ("mcwws-web-status".equals(name)) {
            if (webProcess.isRunning()) {
                Long pid = webProcess.pid();
                int port = getPluginConfig().getInt("port", 8002);
                sender.sendMessage("§a[MCWWS] 网页服务运行中 pid=" + pid + " port=" + port);
                sender.sendMessage("§7目录: " + resolveWebDirectory().getAbsolutePath());
            } else {
                sender.sendMessage("§c[MCWWS] 网页服务未运行。");
                String err = webProcess.getLastStartError();
                if (err != null && !err.isBlank()) {
                    sender.sendMessage("§7原因: " + err);
                }
                Integer exit = webProcess.getLastExitCode();
                if (exit != null) {
                    sender.sendMessage("§7上次退出码: " + exit);
                }
                sender.sendMessage("§7可尝试: §f/mcwws-web-reload");
            }
            return true;
        }
        return false;
    }
}
