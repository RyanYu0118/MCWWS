package work.mcwws.ideachievements;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import com.hm.achievement.api.AdvancedAchievementsAPI;
import com.hm.achievement.api.AdvancedAchievementsAPIFetcher;

public final class McwwsIdeaAchievementsPlugin extends JavaPlugin {

    private AdvancedAchievementsAPI aa;
    private IdeaConfig ideaConfig;
    private PlayerProgressStore progressStore;
    private EconomyFlowService economyFlow;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocal();

        Optional<AdvancedAchievementsAPI> api = AdvancedAchievementsAPIFetcher.fetchInstance();
        if (api.isEmpty()) {
            getLogger().severe("AdvancedAchievements API 不可用，插件已禁用。");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.aa = api.get();

        Bukkit.getPluginManager().registerEvents(new CombatAchievementListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerSessionListener(this), this);

        if (Bukkit.getPluginManager().getPlugin("BankPlus") != null) {
            try {
                Bukkit.getPluginManager().registerEvents(new BankPlusMoneyListener(this), this);
                getLogger().info("已挂钩 BankPlus 交易事件。");
            } catch (NoClassDefFoundError | Exception t) {
                getLogger().log(Level.WARNING, "BankPlus 挂钩失败（可忽略）", t);
            }
        }

        int interval = Math.max(20, ideaConfig.darkChickenIntervalTicks());
        Bukkit.getScheduler().runTaskTimer(this, new DarkChickenTask(this), interval, interval);

        PluginCommand cmd = getCommand("mcwws-ideaach");
        if (cmd != null) {
            IdeaAchCommand executor = new IdeaAchCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("MCWWS_IdeaAchievements 已启用（服主=" + ideaConfig.ownerName() + "）。");
    }

    @Override
    public void onDisable() {
        if (progressStore != null) {
            progressStore.save();
        }
    }

    public void reloadLocal() {
        reloadConfig();
        this.ideaConfig = IdeaConfig.from(getConfig());
        if (this.progressStore == null) {
            this.progressStore = new PlayerProgressStore(this);
        } else {
            this.progressStore.reload();
        }
        this.economyFlow = new EconomyFlowService(this);
    }

    public AdvancedAchievementsAPI aa() {
        return aa;
    }

    public IdeaConfig ideaConfig() {
        return ideaConfig;
    }

    public PlayerProgressStore progressStore() {
        return progressStore;
    }

    public EconomyFlowService economyFlow() {
        return economyFlow;
    }

    public UUID ownerUuid() {
        return ideaConfig.ownerUuid();
    }
}
