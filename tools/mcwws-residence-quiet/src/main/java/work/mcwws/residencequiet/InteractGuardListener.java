package work.mcwws.residencequiet;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Residence 6.0.2.4 在 Paper 26.2 上判定出没权限、发了提示，却不取消交互，这里补上真正的拦截。
 */
final class InteractGuardListener implements Listener {

    private final McwwsResidenceQuietPlugin plugin;

    InteractGuardListener(McwwsResidenceQuietPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Player player = event.getPlayer();
        boolean denySeen = plugin.denySignal().seenThisTick(player.getUniqueId());
        Verdict verdict = evaluate(player, block.getLocation(), flagFor(block));
        if (plugin.guardDebug() && (denySeen || verdict != null)) {
            plugin.getLogger().info("[guard-debug] interact " + player.getName()
                    + " block=" + block.getType()
                    + " flag=" + (verdict == null ? "-" : verdict.flag)
                    + " res=" + (verdict == null ? "-" : verdict.residence)
                    + " allowed=" + (verdict == null ? "-" : verdict.allowed)
                    + " resAdmin=" + (verdict != null && verdict.admin)
                    + " denySeen=" + denySeen
                    + " cancelled=" + event.isCancelled());
        }
        if (event.isCancelled()) {
            return;
        }
        if (shouldBlock(event.getPlayer(), verdict, denySeen)) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.isCancelled() || !plugin.guardFollowDenyMessage()) {
            return;
        }
        if (plugin.denySignal().seenThisTick(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!plugin.guardEnforceContainer() || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        Location location = event.getInventory().getLocation();
        if (location == null) {
            return;
        }
        Verdict verdict = evaluate(player, location, Flags.container);
        if (verdict == null) {
            return;
        }
        if (plugin.guardDebug()) {
            plugin.getLogger().info("[guard-debug] open " + player.getName()
                    + " type=" + event.getInventory().getType()
                    + " res=" + verdict.residence
                    + " allowed=" + verdict.allowed
                    + " resAdmin=" + verdict.admin);
        }
        if (!verdict.allowed && !verdict.admin) {
            event.setCancelled(true);
        }
    }

    private boolean shouldBlock(Player player, Verdict verdict, boolean denySeen) {
        if (verdict != null && !verdict.allowed && !verdict.admin) {
            if (verdict.flag == Flags.container && plugin.guardEnforceContainer()) {
                return true;
            }
            if (verdict.flag == Flags.door && plugin.guardEnforceDoor()) {
                // 潜行 + 手持物品是放置方块，交给 build/place 那套检查
                return !(player.isSneaking() && !player.getInventory().getItemInMainHand().getType().isAir());
            }
        }
        return denySeen && plugin.guardFollowDenyMessage();
    }

    /** @return null 表示这个方块不在本插件直接管的旗标里，只能靠拒绝提示兜底 */
    private static Flags flagFor(Block block) {
        Material type = block.getType();
        if (Tag.DOORS.isTagged(type) || Tag.TRAPDOORS.isTagged(type) || Tag.FENCE_GATES.isTagged(type)) {
            return Flags.door;
        }
        if (block.getState(false) instanceof Container) {
            return Flags.container;
        }
        return null;
    }

    /** @return null 表示不在领地里，或没有对应旗标 */
    private Verdict evaluate(Player player, Location location, Flags flag) {
        if (flag == null) {
            return null;
        }
        ClaimedResidence residence;
        try {
            residence = Residence.getInstance().getResidenceManager().getByLoc(location);
        } catch (Throwable ignored) {
            return null;
        }
        if (residence == null) {
            return null;
        }
        boolean useAllowed = residence.getPermissions().playerHas(player, Flags.use, true);
        boolean allowed = residence.getPermissions().playerHas(player, flag, useAllowed);
        boolean admin;
        try {
            admin = Residence.getInstance().isResAdminOn(player);
        } catch (Throwable ignored) {
            admin = false;
        }
        return new Verdict(residence.getName(), flag, allowed, admin);
    }

    private record Verdict(String residence, Flags flag, boolean allowed, boolean admin) {
    }
}
