/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.enchantments.Enchantment
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.yggdrasil.YggdrasilSerializable;
import java.util.regex.Pattern;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.Nullable;

public class EnchantmentType
implements YggdrasilSerializable {
    @Nullable
    private static Parser<Enchantment> ENCHANTMENT_PARSER = null;
    private final Enchantment type;
    private final int level;
    private static final Pattern pattern = Pattern.compile(".+ \\d+");

    private EnchantmentType() {
        this.type = null;
        this.level = -1;
    }

    public EnchantmentType(Enchantment type) {
        assert (type != null);
        this.type = type;
        this.level = -1;
    }

    public EnchantmentType(Enchantment type, int level) {
        assert (type != null);
        this.type = type;
        this.level = level;
    }

    public int getLevel() {
        return this.level == -1 ? 1 : this.level;
    }

    public int getInternalLevel() {
        return this.level;
    }

    public Enchantment getType() {
        return this.type;
    }

    @Deprecated(since="2.3.0", forRemoval=true)
    public boolean has(ItemType item) {
        return item.hasEnchantments(this.type);
    }

    public String toString() {
        return EnchantmentType.getEnchantmentParser().toString(this.type, 0) + (String)(this.level == -1 ? "" : " " + this.level);
    }

    @Nullable
    public static EnchantmentType parse(String s) {
        Parser<Enchantment> enchantmentParser = EnchantmentType.getEnchantmentParser();
        if (pattern.matcher(s).matches()) {
            String name = s.substring(0, s.lastIndexOf(32));
            assert (name != null);
            Enchantment ench = enchantmentParser.parse(name, ParseContext.DEFAULT);
            if (ench == null) {
                return null;
            }
            String level = s.substring(s.lastIndexOf(32) + 1);
            assert (level != null);
            return new EnchantmentType(ench, Utils.parseInt(level));
        }
        Enchantment ench = enchantmentParser.parse(s, ParseContext.DEFAULT);
        if (ench == null) {
            return null;
        }
        return new EnchantmentType(ench, -1);
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + this.level;
        result = 31 * result + this.type.hashCode();
        return result;
    }

    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EnchantmentType)) {
            return false;
        }
        EnchantmentType other = (EnchantmentType)obj;
        if (this.level != other.level) {
            return false;
        }
        return this.type.equals(other.type);
    }

    private static Parser<Enchantment> getEnchantmentParser() {
        if (ENCHANTMENT_PARSER == null) {
            ClassInfo<Enchantment> classInfo = Classes.getExactClassInfo(Enchantment.class);
            if (classInfo == null) {
                throw new IllegalStateException("Enchantment ClassInfo not found");
            }
            ENCHANTMENT_PARSER = classInfo.getParser();
            if (ENCHANTMENT_PARSER == null) {
                throw new IllegalStateException("Enchantment parser not found");
            }
        }
        return ENCHANTMENT_PARSER;
    }
}

