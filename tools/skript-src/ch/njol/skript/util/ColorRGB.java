/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.math.NumberUtils
 *  org.bukkit.Color
 *  org.bukkit.DyeColor
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.util.Color;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Math2;
import ch.njol.yggdrasil.Fields;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.DyeColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColorRGB
implements Color {
    private static final Pattern RGB_PATTERN = Pattern.compile("(?>rgb|RGB) (\\d+), (\\d+), (\\d+)");
    private org.bukkit.Color bukkit;
    @Nullable
    private DyeColor dye;

    @Deprecated(since="2.10.0", forRemoval=true)
    @ApiStatus.Internal
    public ColorRGB(int red, int green, int blue) {
        this(org.bukkit.Color.fromRGB((int)Math2.fit(0, red, 255), (int)Math2.fit(0, green, 255), (int)Math2.fit(0, blue, 255)));
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    @ApiStatus.Internal
    public ColorRGB(org.bukkit.Color bukkit) {
        this.dye = DyeColor.getByColor((org.bukkit.Color)bukkit);
        this.bukkit = bukkit;
    }

    @Contract(value="_,_,_,_ -> new")
    @NotNull
    public static ColorRGB fromRGBA(int red, int green, int blue, int alpha) {
        return new ColorRGB(org.bukkit.Color.fromARGB((int)alpha, (int)red, (int)green, (int)blue));
    }

    @Contract(value="_,_,_ -> new")
    @NotNull
    public static ColorRGB fromRGB(int red, int green, int blue) {
        return new ColorRGB(red, green, blue);
    }

    @Contract(value="_ -> new")
    @NotNull
    public static ColorRGB fromBukkitColor(org.bukkit.Color bukkit) {
        return new ColorRGB(bukkit);
    }

    @Override
    public int getAlpha() {
        return this.bukkit.getAlpha();
    }

    @Override
    public int getRed() {
        return this.bukkit.getRed();
    }

    @Override
    public int getGreen() {
        return this.bukkit.getGreen();
    }

    @Override
    public int getBlue() {
        return this.bukkit.getBlue();
    }

    @Override
    public org.bukkit.Color asBukkitColor() {
        return this.bukkit;
    }

    @Override
    @Nullable
    public DyeColor asDyeColor() {
        return this.dye;
    }

    @Override
    public String getName() {
        String rgb = this.bukkit.getRed() + ", " + this.bukkit.getGreen() + ", " + this.bukkit.getBlue();
        if (this.bukkit.getAlpha() != 255) {
            return "argb " + this.bukkit.getAlpha() + ", " + rgb;
        }
        return "rgb " + rgb;
    }

    @Nullable
    public static ColorRGB fromString(String string) {
        Matcher matcher = RGB_PATTERN.matcher(string);
        if (!matcher.matches()) {
            return null;
        }
        return new ColorRGB(NumberUtils.toInt((String)matcher.group(1)), NumberUtils.toInt((String)matcher.group(2)), NumberUtils.toInt((String)matcher.group(3)));
    }

    @Nullable
    public static ColorRGB fromHexString(String hex) {
        if (((String)hex).length() != 6 && ((String)hex).length() != 8) {
            return null;
        }
        if (((String)hex).length() == 6) {
            hex = "FF" + (String)hex;
        }
        try {
            int argb = Integer.parseUnsignedInt((String)hex, 16);
            return ColorRGB.fromRGBA(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >> 24 & 0xFF);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Fields serialize() throws NotSerializableException {
        return new Fields(this, Variables.yggdrasil);
    }

    @Override
    public void deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
        org.bukkit.Color b = fields.getObject("bukkit", org.bukkit.Color.class);
        DyeColor d = fields.getObject("dye", DyeColor.class);
        if (b == null) {
            return;
        }
        this.dye = d == null ? DyeColor.getByColor((org.bukkit.Color)b) : d;
        this.bukkit = b;
    }
}

