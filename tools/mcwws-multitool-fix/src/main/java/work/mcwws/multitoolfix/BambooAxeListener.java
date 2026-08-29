package work.mcwws.multitoolfix;

import me.darkolythe.multitool.Multitool;
import me.darkolythe.multitool.MultitoolInventory;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * MultitoolPlus 把名字含 BAMBOO 的方块一律切成剑（1.14 起对竹活株是对的）。
 * 竹板、竹块、竹马赛克等制品应按原版用斧。
 */
final class BambooAxeListener implements Listener {

    private final Multitool multitool;

    BambooAxeListener(Multitool multitool) {
        this.multitool = multitool;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !isAxeBambooProduct(block.getType())) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("multitool.use")) {
            return;
        }
        if (!Boolean.TRUE.equals(multitool.multitoolutils.getToggle(player.getUniqueId()))) {
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!multitool.multitoolutils.isTool(hand)) {
            return;
        }

        Inventory toolInv = multitool.multitoolutils.getToolInv(player);
        ItemStack axe = toolInv.getItem(ToolMapPatcher.SLOT_AXE);
        if (axe == null || axe.getType() == Material.GRAY_STAINED_GLASS_PANE || axe.getType() == Material.AIR) {
            return;
        }

        ItemStack give = axe.clone();
        ItemMeta meta = give.getItemMeta();
        if (meta != null) {
            meta.setLore(MultitoolInventory.addLore(meta, multitool.toollore, false));
            give.setItemMeta(meta);
        }
        player.getInventory().setItemInMainHand(give);
        multitool.lastblock.put(player.getUniqueId(), block.getType());
    }

    static boolean isAxeBambooProduct(Material material) {
        return material.name().contains("BAMBOO") && Tag.MINEABLE_AXE.isTagged(material);
    }
}
