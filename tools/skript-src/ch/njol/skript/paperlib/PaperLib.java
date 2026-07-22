/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  org.bukkit.Chunk
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.player.PlayerTeleportEvent$TeleportCause
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.plugin.Plugin
 */
package ch.njol.skript.paperlib;

import ch.njol.skript.paperlib.environments.CraftBukkitEnvironment;
import ch.njol.skript.paperlib.environments.Environment;
import ch.njol.skript.paperlib.environments.PaperEnvironment;
import ch.njol.skript.paperlib.environments.SpigotEnvironment;
import ch.njol.skript.paperlib.features.blockstatesnapshot.BlockStateSnapshotResult;
import ch.njol.skript.paperlib.features.inventoryholdersnapshot.InventoryHolderSnapshotResult;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public class PaperLib {
    private static Environment ENVIRONMENT = PaperLib.initialize();

    private PaperLib() {
    }

    private static Environment initialize() {
        if (PaperLib.hasClass("com.destroystokyo.paper.PaperConfig") || PaperLib.hasClass("io.papermc.paper.configuration.Configuration")) {
            return new PaperEnvironment();
        }
        if (PaperLib.hasClass("org.spigotmc.SpigotConfig")) {
            return new SpigotEnvironment();
        }
        return new CraftBukkitEnvironment();
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Nonnull
    public static Environment getEnvironment() {
        return ENVIRONMENT;
    }

    public static void setCustomEnvironment(@Nonnull Environment environment) {
        ENVIRONMENT = environment;
    }

    @Nonnull
    public static CompletableFuture<Boolean> teleportAsync(@Nonnull Entity entity, @Nonnull Location location) {
        return ENVIRONMENT.teleport(entity, location, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @Nonnull
    public static CompletableFuture<Boolean> teleportAsync(@Nonnull Entity entity, @Nonnull Location location, PlayerTeleportEvent.TeleportCause cause) {
        return ENVIRONMENT.teleport(entity, location, cause);
    }

    @Nonnull
    public static CompletableFuture<Chunk> getChunkAtAsync(@Nonnull Location loc) {
        return PaperLib.getChunkAtAsync(loc.getWorld(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4, true);
    }

    @Nonnull
    public static CompletableFuture<Chunk> getChunkAtAsync(@Nonnull Location loc, boolean gen) {
        return PaperLib.getChunkAtAsync(loc.getWorld(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4, gen);
    }

    @Nonnull
    public static CompletableFuture<Chunk> getChunkAtAsync(@Nonnull World world, int x, int z) {
        return PaperLib.getChunkAtAsync(world, x, z, true);
    }

    @Nonnull
    public static CompletableFuture<Chunk> getChunkAtAsync(@Nonnull World world, int x, int z, boolean gen) {
        return ENVIRONMENT.getChunkAtAsync(world, x, z, gen, false);
    }

    @Nonnull
    public static CompletableFuture<Chunk> getChunkAtAsync(@Nonnull World world, int x, int z, boolean gen, boolean isUrgent) {
        return ENVIRONMENT.getChunkAtAsync(world, x, z, gen, isUrgent);
    }

    @Nonnull
    public static CompletableFuture<Chunk> getChunkAtAsyncUrgently(@Nonnull World world, int x, int z, boolean gen) {
        return ENVIRONMENT.getChunkAtAsync(world, x, z, gen, true);
    }

    public static boolean isChunkGenerated(@Nonnull Location loc) {
        return PaperLib.isChunkGenerated(loc.getWorld(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    public static boolean isChunkGenerated(@Nonnull World world, int x, int z) {
        return ENVIRONMENT.isChunkGenerated(world, x, z);
    }

    @Nonnull
    public static BlockStateSnapshotResult getBlockState(@Nonnull Block block, boolean useSnapshot) {
        return ENVIRONMENT.getBlockState(block, useSnapshot);
    }

    @Nonnull
    public static InventoryHolderSnapshotResult getHolder(@Nonnull Inventory inventory, boolean useSnapshot) {
        return ENVIRONMENT.getHolder(inventory, useSnapshot);
    }

    public static CompletableFuture<Location> getBedSpawnLocationAsync(@Nonnull Player player, boolean isUrgent) {
        return ENVIRONMENT.getBedSpawnLocationAsync(player, isUrgent);
    }

    public static boolean isVersion(int minor) {
        return ENVIRONMENT.isVersion(minor);
    }

    public static boolean isVersion(int minor, int patch) {
        return ENVIRONMENT.isVersion(minor, patch);
    }

    public static int getMinecraftVersion() {
        return ENVIRONMENT.getMinecraftVersion();
    }

    public static int getMinecraftPatchVersion() {
        return ENVIRONMENT.getMinecraftPatchVersion();
    }

    public static int getMinecraftPreReleaseVersion() {
        return ENVIRONMENT.getMinecraftPreReleaseVersion();
    }

    public static int getMinecraftReleaseCandidateVersion() {
        return ENVIRONMENT.getMinecraftReleaseCandidateVersion();
    }

    public static boolean isSpigot() {
        return ENVIRONMENT.isSpigot();
    }

    public static boolean isPaper() {
        return ENVIRONMENT.isPaper();
    }

    public static void suggestPaper(@Nonnull Plugin plugin) {
        PaperLib.suggestPaper(plugin, Level.INFO);
    }

    public static void suggestPaper(@Nonnull Plugin plugin, @Nonnull Level logLevel) {
        if (PaperLib.isPaper()) {
            return;
        }
        String benefitsProperty = "paperlib.shown-benefits";
        String pluginName = plugin.getDescription().getName();
        Logger logger = plugin.getLogger();
        logger.log(logLevel, "====================================================");
        logger.log(logLevel, " " + pluginName + " works better if you use Paper ");
        logger.log(logLevel, " as your server software. ");
        if (System.getProperty("paperlib.shown-benefits") == null) {
            System.setProperty("paperlib.shown-benefits", "1");
            logger.log(logLevel, "  ");
            logger.log(logLevel, " Paper offers significant performance improvements,");
            logger.log(logLevel, " bug fixes, security enhancements and optional");
            logger.log(logLevel, " features for server owners to enhance their server.");
            logger.log(logLevel, "  ");
            logger.log(logLevel, " Paper includes Timings v2, which is significantly");
            logger.log(logLevel, " better at diagnosing lag problems over v1.");
            logger.log(logLevel, "  ");
            logger.log(logLevel, " All of your plugins should still work, and the");
            logger.log(logLevel, " Paper community will gladly help you fix any issues.");
            logger.log(logLevel, "  ");
            logger.log(logLevel, " Join the Paper Community @ https://papermc.io");
        }
        logger.log(logLevel, "====================================================");
    }
}

