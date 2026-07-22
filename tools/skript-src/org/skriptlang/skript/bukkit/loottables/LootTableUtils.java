/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.loot.LootTable
 *  org.bukkit.loot.Lootable
 */
package org.skriptlang.skript.bukkit.loottables;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;

public class LootTableUtils {
    public static boolean isLootable(Object object) {
        if (object instanceof Block) {
            Block block = (Block)object;
            object = block.getState();
        }
        return object instanceof Lootable;
    }

    public static Lootable getAsLootable(Object object) {
        if (object instanceof Block) {
            Block block = (Block)object;
            object = block.getState();
        }
        if (object instanceof Lootable) {
            Lootable lootable = (Lootable)object;
            return lootable;
        }
        return null;
    }

    public static LootTable getLootTable(Object object) {
        return LootTableUtils.getAsLootable(object).getLootTable();
    }

    public static void updateState(Lootable lootable) {
        if (lootable instanceof BlockState) {
            BlockState blockState = (BlockState)lootable;
            blockState.update(true, false);
        }
    }
}

