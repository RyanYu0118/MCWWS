package work.mcwws.residencequiet;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResAdmin;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Residence 6.0.2.4 在 Paper 26.2 上判定出没权限、发了提示，却不取消交互，这里补上真正的拦截。
 *
 * <p>只处理「使用方块」这类交互。放置与破坏仍由 Residence 自己的 BlockPlace / BlockBreak 检查负责，
 * 否则会把放置掐在 interact 阶段，连「没有放置权限」的提示都发不出来。
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
        if (isResidenceBypass(player)) {
            if (plugin.guardDebug()) {
                plugin.getLogger().info("[guard-debug] skip admin/op interact " + player.getName()
                        + " block=" + block.getType());
            }
            return;
        }
        Flags flag = flagFor(block);
        if (flag == null) {
            return;
        }
        boolean denySeen = plugin.denySignal().seenThisTick(player.getUniqueId());
        Verdict verdict = evaluate(player, block.getLocation(), flag);
        if (verdict == null) {
            return;
        }
        boolean placing = isPlacementAttempt(player);
        if (plugin.guardDebug()) {
            plugin.getLogger().info("[guard-debug] interact " + player.getName()
                    + " block=" + block.getType()
                    + " flag=" + flag
                    + " res=" + verdict.residence
                    + " allowed=" + verdict.allowed
                    + " resAdmin=" + verdict.admin
                    + " placing=" + placing
                    + " denySeen=" + denySeen
                    + " cancelled=" + event.isCancelled());
        }
        if (event.isCancelled()) {
            return;
        }
        // 潜行放置不要拦；采浆果即使潜行也要按 harvest 检查
        if (placing && flag != Flags.harvest) {
            return;
        }
        boolean block1 = plugin.guardEnforce() && !verdict.allowed && !verdict.admin;
        boolean block2 = plugin.guardFollowDenyMessage() && denySeen;
        if (block1 || block2) {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        if (!plugin.guardEnforce()) {
            return;
        }
        Player player = event.getPlayer();
        if (isResidenceBypass(player)) {
            return;
        }
        Block block = event.getHarvestedBlock();
        if (block == null) {
            return;
        }
        Verdict verdict = evaluate(player, block.getLocation(), Flags.harvest);
        if (verdict == null) {
            return;
        }
        if (plugin.guardDebug()) {
            plugin.getLogger().info("[guard-debug] harvest " + player.getName()
                    + " block=" + block.getType()
                    + " res=" + verdict.residence
                    + " allowed=" + verdict.allowed
                    + " resAdmin=" + verdict.admin);
        }
        if (!verdict.allowed && !verdict.admin) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.isCancelled() || !plugin.guardFollowDenyMessage()) {
            return;
        }
        if (isResidenceBypass(event.getPlayer())) {
            return;
        }
        if (plugin.denySignal().seenThisTick(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!plugin.guardEnforce() || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (isResidenceBypass(player)) {
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

    /** 潜行且手上有东西时右键是放置方块，交给 Residence 的放置检查 */
    private static boolean isPlacementAttempt(Player player) {
        return player.isSneaking()
                && !player.getInventory().getItemInMainHand().getType().isAir();
    }

    /** @return null 表示这个方块不算「使用」类交互（例如普通建材，右键就是放置） */
    private static Flags flagFor(Block block) {
        Material type = block.getType();
        String name = type.name();
        if (Tag.DOORS.isTagged(type) || Tag.TRAPDOORS.isTagged(type) || Tag.FENCE_GATES.isTagged(type)) {
            return Flags.door;
        }
        if (type == Material.SWEET_BERRY_BUSH || type == Material.CAVE_VINES || type == Material.CAVE_VINES_PLANT) {
            return Flags.harvest;
        }
        if (name.endsWith("_BUTTON")) {
            return Flags.button;
        }
        if (name.endsWith("_BED")) {
            return Flags.bed;
        }
        if (name.endsWith("_ANVIL") || type == Material.ANVIL) {
            return Flags.anvil;
        }
        if (type == Material.FLOWER_POT || name.startsWith("POTTED_")) {
            return Flags.flowerpot;
        }
        Flags mapped = switch (type) {
            case LEVER -> Flags.lever;
            case ENCHANTING_TABLE -> Flags.enchant;
            case CRAFTING_TABLE -> Flags.table;
            case BREWING_STAND -> Flags.brew;
            case BEACON -> Flags.beacon;
            case GRINDSTONE -> Flags.grindstone;
            case LOOM -> Flags.loom;
            case SMITHING_TABLE -> Flags.smithing;
            case STONECUTTER -> Flags.stonecutter;
            case CARTOGRAPHY_TABLE -> Flags.cartography;
            case FLETCHING_TABLE -> Flags.fletching;
            case NOTE_BLOCK -> Flags.note;
            case REPEATER, COMPARATOR -> Flags.diode;
            case CAKE -> Flags.cake;
            case JUKEBOX -> Flags.use;
            default -> null;
        };
        if (mapped != null) {
            return mapped;
        }
        return block.getState(false) instanceof Container ? Flags.container : null;
    }

    /** @return null 表示不在领地里，交给世界旗标处理 */
    private Verdict evaluate(Player player, Location location, Flags flag) {
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
        boolean admin = isResidenceBypass(player);
        return new Verdict(residence.getName(), allowed, admin);
    }

    /**
     * 以 Residence 当前的管理员身份为准（OP + AdminOPs，或 residence.admin）。
     * 进服时 OP 会把 /resadmin 拨开，但关掉 OP 后这个开关不会自己关上，这里立刻同步。
     */
    static boolean isResidenceBypass(Player player) {
        return syncResAdminToggle(player);
    }

    /** @return 现在是否应按管理员覆盖领地旗标 */
    static boolean syncResAdminToggle(Player player) {
        if (player == null) {
            return false;
        }
        try {
            Residence residence = Residence.getInstance();
            if (residence == null) {
                return false;
            }
            boolean admin = residence.getPermissionManager().isResidenceAdmin(player);
            boolean toggled = residence.isResAdminOn(player);
            if (admin && !toggled) {
                ResAdmin.turnResAdminOn(player);
            } else if (!admin && toggled) {
                ResAdmin.turnResAdminOff(player);
            }
            return admin;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private record Verdict(String residence, boolean allowed, boolean admin) {
    }
}
