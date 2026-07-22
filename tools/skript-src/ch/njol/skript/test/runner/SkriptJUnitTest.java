/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.GameRule
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Pig
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;

public abstract class SkriptJUnitTest {
    private static String currentJUnitTest;
    private static long delay;

    public static long getShutdownDelay() {
        return delay;
    }

    public static void setShutdownDelay(long delay) {
        SkriptJUnitTest.delay = delay;
    }

    public void cleanup() {
        SkriptJUnitTest.getTestWorld().getEntities().forEach(Entity::remove);
        SkriptJUnitTest.setBlock(Material.AIR);
    }

    public static World getTestWorld() {
        return (World)Bukkit.getWorlds().get(0);
    }

    public static Location getTestLocation() {
        return SkriptJUnitTest.getTestWorld().getSpawnLocation().add(0.0, 1.0, 0.0);
    }

    public static Pig spawnTestPig() {
        return (Pig)SkriptJUnitTest.spawnTestEntity(EntityType.PIG);
    }

    public static <E extends Entity> E spawnTestEntity(EntityType entityType) {
        if ((double)delay <= 0.0) {
            delay = 1L;
        }
        return (E)SkriptJUnitTest.getTestWorld().spawnEntity(SkriptJUnitTest.getTestLocation(), entityType);
    }

    public static Block setBlock(Material material) {
        Block block = SkriptJUnitTest.getBlock();
        block.setType(material);
        return block;
    }

    public static Block getBlock() {
        return SkriptJUnitTest.getTestWorld().getSpawnLocation().add(10.0, 1.0, 0.0).getBlock();
    }

    public static String getCurrentJUnitTest() {
        return currentJUnitTest;
    }

    public static void setCurrentJUnitTest(String currentJUnitTest) {
        SkriptJUnitTest.currentJUnitTest = currentJUnitTest;
    }

    public static void clearJUnitTest() {
        currentJUnitTest = null;
        SkriptJUnitTest.setShutdownDelay(0L);
    }

    static {
        World world = SkriptJUnitTest.getTestWorld();
        world.setGameRule(GameRule.MAX_ENTITY_CRAMMING, (Object)1000);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, (Object)false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, (Object)false);
        world.setGameRule(GameRule.MOB_GRIEFING, (Object)false);
        if (Skript.isRunningMinecraft(1, 15)) {
            world.setGameRule(GameRule.DO_PATROL_SPAWNING, (Object)false);
            world.setGameRule(GameRule.DO_TRADER_SPAWNING, (Object)false);
            world.setGameRule(GameRule.DISABLE_RAIDS, (Object)false);
        }
        delay = 0L;
    }
}

