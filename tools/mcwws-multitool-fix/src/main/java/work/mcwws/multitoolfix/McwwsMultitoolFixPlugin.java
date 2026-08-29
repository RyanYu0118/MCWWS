package work.mcwws.multitoolfix;

import me.darkolythe.multitool.Multitool;
import org.bukkit.plugin.java.JavaPlugin;

public final class McwwsMultitoolFixPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Multitool multitool = Multitool.getInstance();
        if (multitool == null) {
            getLogger().severe("未找到 MultitoolPlus 实例，插件已禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        int filled = ToolMapPatcher.patch(multitool);
        getServer().getPluginManager().registerEvents(new BambooAxeListener(multitool), this);
        getLogger().info("已按原版可挖掘标签补全 " + filled + " 条工具映射；竹制品将改切斧而非剑。");
    }
}
