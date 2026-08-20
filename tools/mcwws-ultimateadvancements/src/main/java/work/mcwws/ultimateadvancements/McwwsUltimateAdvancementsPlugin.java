package work.mcwws.ultimateadvancements;

import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI;
import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.events.PlayerLoadingCompletedEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * L 键只保留「流浪世界」一页。
 * 布局：横向延伸（x 增大）；不同模块占不同行（y），避免竖向拖很长。
 *
 * <pre>
 * y=0  启程 ── 新手入门 ── 第一块石头 ── 第一次打造
 * y=1  （预留：矿区模块横向扩展）
 * y=2  （预留：建造模块横向扩展）
 * </pre>
 */
public final class McwwsUltimateAdvancementsPlugin extends JavaPlugin implements Listener {

    private static final String BG = "textures/gui/advancements/backgrounds/stone.png";

    private AdvancementTab tab;
    private RootAdvancement root;
    private final List<BaseAdvancement> autoGrantHubs = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        tab = UltimateAdvancementAPI.getInstance(this).createAdvancementTab("mcwws_story");

        // --- 根：启程 (0, 0) ---
        AdvancementDisplay rootDisplay = new AdvancementDisplay(
                Material.COMPASS,
                "流浪世界",
                AdvancementFrameType.TASK,
                true,
                true,
                0f,
                0f,
                "从第一步开始：点亮你的启程"
        );
        root = new RootStory(tab, "root", rootDisplay, BG);

        List<BaseAdvancement> nodes = new ArrayList<>();

        // --- 模块行 y=0：新手（横向） ---
        AdvancementDisplay introHubDisplay = new AdvancementDisplay(
                Material.OAK_SIGN,
                "新手入门",
                AdvancementFrameType.TASK,
                true,
                false,
                1f,
                0f,
                "模块：生存第一步"
        );
        BaseAdvancement introHub = new HubStory("module_intro", introHubDisplay, root);
        nodes.add(introHub);
        autoGrantHubs.add(introHub);

        AdvancementDisplay stoneDisplay = new AdvancementDisplay(
                Material.STONE,
                "第一块石头",
                AdvancementFrameType.GOAL,
                true,
                true,
                2f,
                0f,
                "挖到你的第一块石头"
        );
        BaseAdvancement firstStone = new StoneBreakStory("break_first_stone", stoneDisplay, introHub);
        nodes.add(firstStone);

        AdvancementDisplay craftDisplay = new AdvancementDisplay(
                Material.CRAFTING_TABLE,
                "第一次打造",
                AdvancementFrameType.CHALLENGE,
                true,
                true,
                3f,
                0f,
                "通过工作台完成第一次合成"
        );
        BaseAdvancement firstCraft = new CraftAnyStory("craft_first_item", craftDisplay, firstStone);
        nodes.add(firstCraft);

        // --- 模块行 y=1：矿区（占位枢纽，后续向右加节点） ---
        AdvancementDisplay miningHubDisplay = new AdvancementDisplay(
                Material.IRON_PICKAXE,
                "矿区探索",
                AdvancementFrameType.TASK,
                true,
                false,
                1f,
                1f,
                "模块：挖矿与矿区任务（后续扩展）"
        );
        BaseAdvancement miningHub = new HubStory("module_mining", miningHubDisplay, root);
        nodes.add(miningHub);
        autoGrantHubs.add(miningHub);

        // --- 模块行 y=2：建造（占位枢纽） ---
        AdvancementDisplay buildHubDisplay = new AdvancementDisplay(
                Material.BRICKS,
                "建造与改造",
                AdvancementFrameType.TASK,
                true,
                false,
                1f,
                2f,
                "模块：创世神 / Axiom 相关进度（后续扩展）"
        );
        BaseAdvancement buildHub = new HubStory("module_build", buildHubDisplay, root);
        nodes.add(buildHub);
        autoGrantHubs.add(buildHub);

        tab.registerAdvancements(root, nodes.toArray(BaseAdvancement[]::new));
    }

    @EventHandler
    public void onPlayerLoadingCompleted(PlayerLoadingCompletedEvent e) {
        Player p = e.getPlayer();
        if (p == null) {
            return;
        }

        tab.showTab(p);
        if (!root.isGranted(p)) {
            root.grant(p);
        }
        for (BaseAdvancement hub : autoGrantHubs) {
            if (!hub.isGranted(p)) {
                hub.grant(p);
            }
        }
    }

    private static final class RootStory extends RootAdvancement {
        public RootStory(AdvancementTab advancementTab, String key, AdvancementDisplay display, String backgroundTexture) {
            super(advancementTab, key, display, backgroundTexture);
        }

        @Override
        public void giveReward(Player player) {
            player.sendMessage("§a[流浪世界] 已点亮进度：启程。");
        }
    }

    /** 模块枢纽：进服自动点亮，不发聊天刷屏。 */
    private static final class HubStory extends BaseAdvancement {
        public HubStory(String key, AdvancementDisplay display, Advancement parent) {
            super(key, display, parent);
        }

        @Override
        public void giveReward(Player player) {
            // 枢纽节点无奖励，避免刷屏
        }
    }

    private final class StoneBreakStory extends BaseAdvancement {
        public StoneBreakStory(String key, AdvancementDisplay display, Advancement parent) {
            super(key, display, parent);

            registerEvent(BlockBreakEvent.class, e -> {
                Player p = e.getPlayer();
                if (p == null) {
                    return;
                }
                if (isVisible(p) && !isGranted(p) && e.getBlock().getType() == Material.STONE) {
                    grant(p);
                }
            });
        }

        @Override
        public void giveReward(Player player) {
            player.sendMessage("§b[流浪世界] 完成：第一块石头。");
        }
    }

    private final class CraftAnyStory extends BaseAdvancement {
        public CraftAnyStory(String key, AdvancementDisplay display, Advancement parent) {
            super(key, display, parent);

            registerEvent(CraftItemEvent.class, e -> {
                if (!(e.getWhoClicked() instanceof Player p)) {
                    return;
                }

                if (!isVisible(p) || isGranted(p)) {
                    return;
                }

                ItemStack result = e.getRecipe() == null ? null : e.getRecipe().getResult();
                if (result == null || result.getType() == Material.AIR) {
                    return;
                }

                grant(p);
            });
        }

        @Override
        public void giveReward(Player player) {
            player.sendMessage("§d[流浪世界] 完成：第一次打造。");
        }
    }
}
