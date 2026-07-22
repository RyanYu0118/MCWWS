/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.TreeType
 *  org.bukkit.block.Block
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Noun;
import ch.njol.util.coll.CollectionUtils;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

public final class StructureType
extends Enum<StructureType> {
    public static final /* enum */ StructureType TREE = new StructureType(TreeType.TREE, TreeType.BIG_TREE, TreeType.REDWOOD, TreeType.TALL_REDWOOD, TreeType.MEGA_REDWOOD, TreeType.BIRCH, TreeType.TALL_BIRCH, TreeType.SMALL_JUNGLE, TreeType.JUNGLE, TreeType.COCOA_TREE, TreeType.ACACIA, TreeType.DARK_OAK, TreeType.SWAMP);
    public static final /* enum */ StructureType REGULAR = new StructureType(TreeType.TREE, TreeType.BIG_TREE);
    public static final /* enum */ StructureType SMALL_REGULAR = new StructureType(TreeType.TREE);
    public static final /* enum */ StructureType BIG_REGULAR = new StructureType(TreeType.BIG_TREE);
    public static final /* enum */ StructureType REDWOOD = new StructureType(TreeType.REDWOOD, TreeType.TALL_REDWOOD);
    public static final /* enum */ StructureType SMALL_REDWOOD = new StructureType(TreeType.REDWOOD);
    public static final /* enum */ StructureType BIG_REDWOOD = new StructureType(TreeType.TALL_REDWOOD);
    public static final /* enum */ StructureType MEGA_REDWOOD = new StructureType(TreeType.MEGA_REDWOOD);
    public static final /* enum */ StructureType BIRCH = new StructureType(TreeType.BIRCH);
    public static final /* enum */ StructureType TALL_BIRCH = new StructureType(TreeType.TALL_BIRCH);
    public static final /* enum */ StructureType JUNGLE = new StructureType(TreeType.SMALL_JUNGLE, TreeType.JUNGLE);
    public static final /* enum */ StructureType SMALL_JUNGLE = new StructureType(TreeType.SMALL_JUNGLE);
    public static final /* enum */ StructureType BIG_JUNGLE = new StructureType(TreeType.JUNGLE);
    public static final /* enum */ StructureType JUNGLE_BUSH = new StructureType(TreeType.JUNGLE_BUSH);
    public static final /* enum */ StructureType COCOA_TREE = new StructureType(TreeType.COCOA_TREE);
    public static final /* enum */ StructureType ACACIA = new StructureType(TreeType.ACACIA);
    public static final /* enum */ StructureType DARK_OAK = new StructureType(TreeType.DARK_OAK);
    public static final /* enum */ StructureType SWAMP = new StructureType(TreeType.SWAMP);
    public static final /* enum */ StructureType MUSHROOM = new StructureType(TreeType.RED_MUSHROOM, TreeType.BROWN_MUSHROOM);
    public static final /* enum */ StructureType RED_MUSHROOM = new StructureType(TreeType.RED_MUSHROOM);
    public static final /* enum */ StructureType BROWN_MUSHROOM = new StructureType(TreeType.BROWN_MUSHROOM);
    private Noun name;
    private final TreeType[] types;
    static final Map<Pattern, StructureType> parseMap;
    private static final /* synthetic */ StructureType[] $VALUES;

    public static StructureType[] values() {
        return (StructureType[])$VALUES.clone();
    }

    public static StructureType valueOf(String name) {
        return Enum.valueOf(StructureType.class, name);
    }

    private StructureType(TreeType ... types) {
        this.types = types;
        this.name = new Noun("tree types." + this.name() + ".name");
    }

    public void grow(Location loc) {
        TreeType tree = CollectionUtils.getRandom(this.types);
        assert (tree != null);
        loc.getWorld().generateTree(loc, tree);
    }

    public void grow(Block b) {
        TreeType tree = CollectionUtils.getRandom(this.types);
        assert (tree != null);
        b.getWorld().generateTree(b.getLocation(), tree);
    }

    public TreeType[] getTypes() {
        return this.types;
    }

    public String toString() {
        return this.name.toString();
    }

    public String toString(int flags) {
        return this.name.toString(flags);
    }

    public Noun getName() {
        return this.name;
    }

    public boolean is(TreeType type) {
        return CollectionUtils.contains(this.types, type);
    }

    @Nullable
    public static StructureType fromName(String s) {
        if (parseMap.isEmpty()) {
            for (StructureType t : StructureType.values()) {
                String pattern = Language.get("tree types." + t.name() + ".pattern");
                parseMap.put(Pattern.compile(pattern, 2), t);
            }
        }
        s = ((String)s).toLowerCase(Locale.ENGLISH);
        for (Map.Entry entry : parseMap.entrySet()) {
            if (!((Pattern)entry.getKey()).matcher((CharSequence)s).matches()) continue;
            return (StructureType)((Object)entry.getValue());
        }
        return null;
    }

    private static /* synthetic */ StructureType[] $values() {
        return new StructureType[]{TREE, REGULAR, SMALL_REGULAR, BIG_REGULAR, REDWOOD, SMALL_REDWOOD, BIG_REDWOOD, MEGA_REDWOOD, BIRCH, TALL_BIRCH, JUNGLE, SMALL_JUNGLE, BIG_JUNGLE, JUNGLE_BUSH, COCOA_TREE, ACACIA, DARK_OAK, SWAMP, MUSHROOM, RED_MUSHROOM, BROWN_MUSHROOM};
    }

    static {
        $VALUES = StructureType.$values();
        parseMap = new HashMap<Pattern, StructureType>();
        Language.addListener(parseMap::clear);
    }
}

