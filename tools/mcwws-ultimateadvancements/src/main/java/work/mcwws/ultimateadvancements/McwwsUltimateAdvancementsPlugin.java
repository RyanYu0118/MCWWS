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

public final class McwwsUltimateAdvancementsPlugin extends JavaPlugin implements Listener {

    private AdvancementTab tab;
    private RootAdvancement root;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // namespace 建议唯一；玩家会在 L 键进度树里看到该 tab。
        tab = UltimateAdvancementAPI.getInstance(this).createAdvancementTab("mcwws_story");

        AdvancementDisplay rootDisplay = new AdvancementDisplay(
                Material.COMPASS,
                "流浪世界",
                AdvancementFrameType.TASK,
                true,
                true,
                0,
                0,
                "从第一步开始：点亮你的启程"
        );

        root = new RootStory(tab, "root", rootDisplay, "textures/block/raw_iron_block.png");

        AdvancementDisplay stoneDisplay = new AdvancementDisplay(
                Material.STONE,
                "第一块石头",
                AdvancementFrameType.GOAL,
                true,
                true,
                1,
                0,
                "挖到你的第一块石头"
        );
        BaseAdvancement firstStone = new StoneBreakStory("break_first_stone", stoneDisplay, root);

        AdvancementDisplay craftDisplay = new AdvancementDisplay(
                Material.CRAFTING_TABLE,
                "第一次打造",
                AdvancementFrameType.CHALLENGE,
                true,
                true,
                2,
                0,
                "通过工作台完成第一次合成"
        );
        BaseAdvancement firstCraft = new CraftAnyStory("craft_first_item", craftDisplay, root);

        tab.registerAdvancements(root, firstStone, firstCraft);
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

    private final class StoneBreakStory extends BaseAdvancement {
        public StoneBreakStory(String key, AdvancementDisplay display, Advancement parent) {
            super(key, display, parent);

            // 任意玩家挖到第一块石头后，点亮该节点。
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

