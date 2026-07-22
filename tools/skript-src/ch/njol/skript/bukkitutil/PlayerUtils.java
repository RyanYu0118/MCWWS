/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil;

import ch.njol.skript.Skript;
import ch.njol.skript.util.Task;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class PlayerUtils {
    private static final Set<Player> inventoryUpdateList = Collections.synchronizedSet(new HashSet());

    public static void updateInventory(@Nullable Player player) {
        if (player != null) {
            inventoryUpdateList.add(player);
        }
    }

    @Deprecated(since="2.7.0", forRemoval=true)
    public static Collection<? extends Player> getOnlinePlayers() {
        return ImmutableList.copyOf((Collection)Bukkit.getOnlinePlayers());
    }

    public static boolean canEat(Player p, Material food) {
        GameMode gm = p.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) {
            return false;
        }
        boolean edible = food.isEdible();
        if (!edible) {
            return false;
        }
        return p.getFoodLevel() < 20 || (switch (food) {
            case Material.GOLDEN_APPLE, Material.CHORUS_FRUIT -> true;
            default -> false;
        });
    }

    public static int getLevelXP(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        }
        if (level <= 30) {
            return 5 * level - 38;
        }
        return 9 * level - 158;
    }

    public static int getCumulativeXP(int level) {
        if (level <= 15) {
            return level * level + 6 * level;
        }
        if (level <= 30) {
            return (int)(2.5 * (double)(level * level) - 40.5 * (double)level + 360.0);
        }
        return (int)(4.5 * (double)(level * level) - 162.5 * (double)level + 2220.0);
    }

    public static int getTotalXP(int level, double progress) {
        return (int)((double)PlayerUtils.getCumulativeXP(level) + (double)PlayerUtils.getLevelXP(level) * progress);
    }

    public static int getTotalXP(Player player) {
        return PlayerUtils.getTotalXP(player.getLevel(), player.getExp());
    }

    public static void setTotalXP(Player player, int experience) {
        int level = 0;
        if (experience < 0) {
            experience = 0;
        }
        while (experience >= PlayerUtils.getLevelXP(level)) {
            experience -= PlayerUtils.getLevelXP(level);
            ++level;
        }
        player.setLevel(level);
        player.setExp((float)experience / (float)PlayerUtils.getLevelXP(level));
    }

    static {
        new Task((Plugin)Skript.getInstance(), 1L, 1L){

            @Override
            public void run() {
                for (Player p : inventoryUpdateList) {
                    p.updateInventory();
                }
                inventoryUpdateList.clear();
            }
        };
    }
}

