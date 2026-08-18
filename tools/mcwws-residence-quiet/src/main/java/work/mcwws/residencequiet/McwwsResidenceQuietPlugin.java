package work.mcwws.residencequiet;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class McwwsResidenceQuietPlugin extends JavaPlugin {

    private DenyThrottle throttle;
    private DenyHud hud;
    private PacketDenyFilter packetFilter;
    private final DenySignal denySignal = new DenySignal();
    private boolean debug;
    private boolean guardDebug;
    private boolean guardEnforce;
    private boolean guardFollowDenyMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocal();
        getServer().getPluginManager().registerEvents(new ResidenceVisitListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractGuardListener(this), this);
        getServer().getPluginManager().registerEvents(new OpAdminSyncListener(this), this);
        getServer().getOnlinePlayers().forEach(InteractGuardListener::syncResAdminToggle);
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            packetFilter = new PacketDenyFilter(this);
            packetFilter.register();
            getLogger().info("已挂接 ProtocolLib：拒绝提示显示为带倒计时的 Boss 栏，倒计时未结束前不重复叠加。");
        } else {
            getLogger().warning("未找到 ProtocolLib，无法拦截领地拒绝刷屏。");
        }
    }

    @Override
    public void onDisable() {
        if (packetFilter != null) {
            packetFilter.unregister();
            packetFilter = null;
        }
        if (hud != null) {
            hud.shutdown();
            hud = null;
        }
        if (throttle != null) {
            throttle.clearAll();
        }
        denySignal.clearAll();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"mcwws-resquiet-reload".equalsIgnoreCase(command.getName())) {
            return false;
        }
        reloadLocal();
        sender.sendMessage(Component.text("MCWWS_ResidenceQuiet reloaded."));
        return true;
    }

    void reloadLocal() {
        reloadConfig();
        if (hud != null) {
            hud.shutdown();
        }
        throttle = DenyThrottle.fromConfig(getConfig());
        hud = DenyHud.fromConfig(this, getConfig());
        hud.start();
        debug = getConfig().getBoolean("debug", false);
        guardDebug = getConfig().getBoolean("interact-guard.debug", false);
        guardEnforce = getConfig().getBoolean("interact-guard.enforce", true);
        guardFollowDenyMessage = getConfig().getBoolean("interact-guard.follow-deny-message", true);
    }

    DenyThrottle throttle() {
        return throttle;
    }

    DenySignal denySignal() {
        return denySignal;
    }

    boolean debug() {
        return debug;
    }

    boolean guardDebug() {
        return guardDebug;
    }

    boolean guardEnforce() {
        return guardEnforce;
    }

    boolean guardFollowDenyMessage() {
        return guardFollowDenyMessage;
    }

    DenyHud hud() {
        return hud;
    }
}
