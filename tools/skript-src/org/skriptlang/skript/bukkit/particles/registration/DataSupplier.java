/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Material
 *  org.bukkit.block.BlockFace
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.particles.registration;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Direction;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface DataSupplier<D> {
    @Nullable
    public D getData(@Nullable Event var1, Expression<?>[] var2, SkriptParser.ParseResult var3);

    @Nullable
    public static Material getMaterialData(Event event, Expression<?> @NotNull [] expressions, SkriptParser.ParseResult parseResult) {
        Object input = expressions[0].getSingle(event);
        if (!(input instanceof ItemType)) {
            return null;
        }
        ItemType itemType = (ItemType)input;
        return itemType.getMaterial();
    }

    @Nullable
    public static BlockFace getBlockFaceData(Event event, Expression<?> @NotNull [] expressions, SkriptParser.ParseResult parseResult) {
        Object input = expressions[0].getSingle(event);
        if (!(input instanceof Direction)) {
            return null;
        }
        Direction direction = (Direction)input;
        return Direction.toNearestBlockFace(direction.getDirection());
    }

    @Nullable
    public static BlockFace getCartesianBlockFaceData(Event event, Expression<?> @NotNull [] expressions, SkriptParser.ParseResult parseResult) {
        Object input = expressions[0].getSingle(event);
        if (!(input instanceof Direction)) {
            return null;
        }
        Direction direction = (Direction)input;
        return Direction.toNearestCartesianBlockFace(direction.getDirection());
    }

    @Nullable
    public static BlockData getBlockData(Event event, Expression<?> @NotNull [] expressions, SkriptParser.ParseResult parseResult) {
        Object input = expressions[0].getSingle(event);
        if (input instanceof ItemType) {
            ItemType itemType = (ItemType)input;
            return itemType.getMaterial().createBlockData();
        }
        if (input instanceof BlockData) {
            BlockData blockData = (BlockData)input;
            return blockData;
        }
        return null;
    }

    @Nullable
    public static Color getColorData(Event event, Expression<?> @NotNull [] expressions, SkriptParser.ParseResult parseResult) {
        Object input = expressions[0].getSingle(event);
        if (!(input instanceof ch.njol.skript.util.Color)) {
            return null;
        }
        ch.njol.skript.util.Color color = (ch.njol.skript.util.Color)input;
        return color.asBukkitColor();
    }

    public static boolean isOminous(Event event, Expression<?>[] expressions, @NotNull SkriptParser.ParseResult parseResult) {
        return parseResult.hasTag("ominous");
    }

    public static int getNumberDefault10(Event event, Expression<?> @NotNull [] expressions, SkriptParser.ParseResult parseResult) {
        if (expressions[0] == null) {
            return 10;
        }
        Object input = expressions[0].getSingle(event);
        if (!(input instanceof Number)) {
            return 10;
        }
        Number number = (Number)input;
        return number.intValue();
    }

    public static int getNumberDefault1(Event event, Expression<?> @NotNull [] expressions, SkriptParser.ParseResult parseResult) {
        if (expressions[0] == null) {
            return 1;
        }
        Object input = expressions[0].getSingle(event);
        if (!(input instanceof Number)) {
            return 1;
        }
        Number number = (Number)input;
        return number.intValue();
    }
}

