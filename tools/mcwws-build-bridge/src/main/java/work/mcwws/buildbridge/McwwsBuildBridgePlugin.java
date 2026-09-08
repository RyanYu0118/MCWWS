package work.mcwws.buildbridge;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.logging.Level;

public final class McwwsBuildBridgePlugin extends JavaPlugin {

    private PlayerQueryService playerQueryService;
    private BlockWriteService blockWriteService;
    private FaweBridgeService faweBridgeService;
    private LocalHttpServer httpServer;
    private String httpToken = "";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureToken();
        playerQueryService = new PlayerQueryService(this);
        blockWriteService = new BlockWriteService(this, playerQueryService);
        faweBridgeService = new FaweBridgeService(this, playerQueryService);
        BuildApiHandler handler = new BuildApiHandler(playerQueryService, blockWriteService, faweBridgeService);
        httpServer = new LocalHttpServer(this, handler);
        try {
            httpServer.start();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to start BuildBridge HTTP", e);
        }
        getLogger().info("MCWWS_BuildBridge 已启用。FAWE=" + faweBridgeService.isAvailable());
    }

    @Override
    public void onDisable() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"mcwws-buildbridge-reload".equalsIgnoreCase(command.getName())) {
            return false;
        }
        if (!sender.hasPermission("mcwws.buildbridge.admin")) {
            sender.sendMessage("§c缺少权限。");
            return true;
        }
        reloadConfig();
        ensureToken();
        try {
            if (httpServer != null) {
                httpServer.start();
            }
            sender.sendMessage("§aBuildBridge 已重载，HTTP 已重启。");
        } catch (IOException e) {
            sender.sendMessage("§cHTTP 重启失败: " + e.getMessage());
            getLogger().log(Level.SEVERE, "Reload HTTP failed", e);
        }
        return true;
    }

    public String getHttpToken() {
        return httpToken;
    }

    private void ensureToken() {
        String token = getConfig().getString("http.token", "");
        if (token == null || token.isBlank()) {
            byte[] bytes = new byte[24];
            new SecureRandom().nextBytes(bytes);
            token = HexFormat.of().formatHex(bytes);
            getConfig().set("http.token", token);
            saveConfig();
            getLogger().warning("已生成 BuildBridge HTTP token，写入 plugins/MCWWS_BuildBridge/config.yml");
        }
        this.httpToken = token;
    }
}
