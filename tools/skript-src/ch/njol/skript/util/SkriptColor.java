/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.Color
 *  org.bukkit.DyeColor
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.localization.Adjective;
import ch.njol.skript.localization.Language;
import ch.njol.skript.util.Color;
import ch.njol.skript.variables.Variables;
import ch.njol.yggdrasil.Fields;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SkriptColor
extends Enum<SkriptColor>
implements Color {
    public static final /* enum */ SkriptColor BLACK = new SkriptColor(DyeColor.BLACK, ChatColor.BLACK);
    public static final /* enum */ SkriptColor DARK_GREY = new SkriptColor(DyeColor.GRAY, ChatColor.DARK_GRAY);
    public static final /* enum */ SkriptColor LIGHT_GREY = new SkriptColor(DyeColor.LIGHT_GRAY, ChatColor.GRAY);
    public static final /* enum */ SkriptColor WHITE = new SkriptColor(DyeColor.WHITE, ChatColor.WHITE);
    public static final /* enum */ SkriptColor DARK_BLUE = new SkriptColor(DyeColor.BLUE, ChatColor.DARK_BLUE);
    public static final /* enum */ SkriptColor BROWN = new SkriptColor(DyeColor.BROWN, ChatColor.BLUE);
    public static final /* enum */ SkriptColor DARK_CYAN = new SkriptColor(DyeColor.CYAN, ChatColor.DARK_AQUA);
    public static final /* enum */ SkriptColor LIGHT_CYAN = new SkriptColor(DyeColor.LIGHT_BLUE, ChatColor.AQUA);
    public static final /* enum */ SkriptColor DARK_GREEN = new SkriptColor(DyeColor.GREEN, ChatColor.DARK_GREEN);
    public static final /* enum */ SkriptColor LIGHT_GREEN = new SkriptColor(DyeColor.LIME, ChatColor.GREEN);
    public static final /* enum */ SkriptColor YELLOW = new SkriptColor(DyeColor.YELLOW, ChatColor.YELLOW);
    public static final /* enum */ SkriptColor ORANGE = new SkriptColor(DyeColor.ORANGE, ChatColor.GOLD);
    public static final /* enum */ SkriptColor DARK_RED = new SkriptColor(DyeColor.RED, ChatColor.DARK_RED);
    public static final /* enum */ SkriptColor LIGHT_RED = new SkriptColor(DyeColor.PINK, ChatColor.RED);
    public static final /* enum */ SkriptColor DARK_PURPLE = new SkriptColor(DyeColor.PURPLE, ChatColor.DARK_PURPLE);
    public static final /* enum */ SkriptColor LIGHT_PURPLE = new SkriptColor(DyeColor.MAGENTA, ChatColor.LIGHT_PURPLE);
    private static final Map<String, SkriptColor> names;
    private static final Set<SkriptColor> colors;
    private static final String LANGUAGE_NODE = "colors";
    private static final Map<Character, SkriptColor> BY_CHAR;
    private ChatColor chat;
    private DyeColor dye;
    @Nullable
    private Adjective adjective;
    private static final /* synthetic */ SkriptColor[] $VALUES;

    public static SkriptColor[] values() {
        return (SkriptColor[])$VALUES.clone();
    }

    public static SkriptColor valueOf(String name) {
        return Enum.valueOf(SkriptColor.class, name);
    }

    private SkriptColor(DyeColor dye, ChatColor chat) {
        this.chat = chat;
        this.dye = dye;
    }

    @Override
    public org.bukkit.Color asBukkitColor() {
        return this.dye.getColor();
    }

    @Override
    public int getAlpha() {
        return this.dye.getColor().getAlpha();
    }

    @Override
    public int getRed() {
        return this.dye.getColor().getRed();
    }

    @Override
    public int getGreen() {
        return this.dye.getColor().getGreen();
    }

    @Override
    public int getBlue() {
        return this.dye.getColor().getBlue();
    }

    @Override
    public DyeColor asDyeColor() {
        return this.dye;
    }

    @Override
    public String getName() {
        assert (this.adjective != null);
        return this.adjective.toString();
    }

    @Override
    public Fields serialize() throws NotSerializableException {
        return new Fields(this, Variables.yggdrasil);
    }

    @Override
    public void deserialize(@NotNull Fields fields) throws StreamCorruptedException {
        this.dye = fields.getObject("dye", DyeColor.class);
        this.chat = fields.getObject("chat", ChatColor.class);
        try {
            this.adjective = fields.getObject("adjective", Adjective.class);
        }
        catch (StreamCorruptedException streamCorruptedException) {
            // empty catch block
        }
    }

    public String getFormattedChat() {
        return String.valueOf(this.chat);
    }

    @Nullable
    public Adjective getAdjective() {
        return this.adjective;
    }

    public ChatColor asChatColor() {
        return this.chat;
    }

    @Deprecated(since="2.3.6", forRemoval=true)
    public byte getWoolData() {
        return this.dye.getWoolData();
    }

    @Deprecated(since="2.3.6", forRemoval=true)
    public byte getDyeData() {
        return (byte)(15 - this.dye.getWoolData());
    }

    private void setAdjective(@Nullable Adjective adjective) {
        this.adjective = adjective;
    }

    @Nullable
    public static SkriptColor fromName(String name) {
        return names.get(name);
    }

    public static SkriptColor fromDyeColor(DyeColor dye) {
        for (SkriptColor color : colors) {
            DyeColor c = color.asDyeColor();
            assert (c != null);
            if (!c.equals((Object)dye)) continue;
            return color;
        }
        assert (false);
        return null;
    }

    public static SkriptColor fromBukkitColor(org.bukkit.Color color) {
        for (SkriptColor c : colors) {
            if (!c.asBukkitColor().equals((Object)color) && !c.asDyeColor().getFireworkColor().equals((Object)color)) continue;
            return c;
        }
        return null;
    }

    @Deprecated(since="2.3.6", forRemoval=true)
    @Nullable
    public static SkriptColor fromDyeData(short data) {
        if (data < 0 || data >= 16) {
            return null;
        }
        for (SkriptColor color : colors) {
            DyeColor c = color.asDyeColor();
            assert (c != null);
            if (c.getDyeData() != data) continue;
            return color;
        }
        return null;
    }

    @Deprecated(since="2.3.6", forRemoval=true)
    @Nullable
    public static SkriptColor fromWoolData(short data) {
        if (data < 0 || data >= 16) {
            return null;
        }
        for (SkriptColor color : colors) {
            DyeColor c = color.asDyeColor();
            assert (c != null);
            if (c.getWoolData() != data) continue;
            return color;
        }
        return null;
    }

    public static String replaceColorChar(String s) {
        return s.replace('\u00a7', '&');
    }

    @Nullable
    public static SkriptColor fromColorChar(char character) {
        return BY_CHAR.get(Character.valueOf(character));
    }

    public String toString() {
        return this.adjective == null ? this.name() : this.adjective.toString(-1, 0);
    }

    private static /* synthetic */ SkriptColor[] $values() {
        return new SkriptColor[]{BLACK, DARK_GREY, LIGHT_GREY, WHITE, DARK_BLUE, BROWN, DARK_CYAN, LIGHT_CYAN, DARK_GREEN, LIGHT_GREEN, YELLOW, ORANGE, DARK_RED, LIGHT_RED, DARK_PURPLE, LIGHT_PURPLE};
    }

    static {
        $VALUES = SkriptColor.$values();
        names = new HashMap<String, SkriptColor>();
        colors = new HashSet<SkriptColor>();
        BY_CHAR = new HashMap<Character, SkriptColor>();
        colors.addAll(Arrays.asList(SkriptColor.values()));
        Language.addListener(() -> {
            names.clear();
            for (SkriptColor color : SkriptColor.values()) {
                String node = "colors." + color.name();
                color.setAdjective(new Adjective(node + ".adjective"));
                for (String name : Language.getList(node + ".names")) {
                    names.put(name.toLowerCase(Locale.ENGLISH), color);
                }
                BY_CHAR.put(Character.valueOf(color.asChatColor().getChar()), color);
            }
        });
        Variables.yggdrasil.registerSingleClass(SkriptColor.class, "SkriptColor");
        Variables.yggdrasil.registerSingleClass(DyeColor.class, "DyeColor");
    }
}

