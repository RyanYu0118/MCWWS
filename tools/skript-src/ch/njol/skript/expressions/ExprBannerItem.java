/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.block.banner.PatternType
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.banner.PatternType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Banner Pattern Item")
@Description(value={"Gets the item from a banner pattern type.", "Note that not all banner pattern types have an item."})
@Example.Examples(value={@Example(value="set {_item} to creeper charged banner pattern item"), @Example(value="set {_item} to snout banner pattern item"), @Example(value="set {_item} to thing banner pattern item")})
@Since(value={"2.10"})
public class ExprBannerItem
extends SimpleExpression<ItemType> {
    private static final Map<Object, Material> bannerMaterials;
    private static boolean PATTERN_TYPE_IS_REGISTRY;
    private PatternType[] patternTypes;
    private Literal<PatternType> literalPattern;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.literalPattern = (Literal)exprs[0];
        for (PatternType type : this.patternTypes = this.literalPattern.getArray()) {
            if (bannerMaterials.containsKey(type)) continue;
            Skript.error("There is no item for the banner pattern type '" + String.valueOf(type) + "'.");
            return false;
        }
        return true;
    }

    protected ItemType @Nullable [] get(Event event) {
        ArrayList<ItemType> itemTypes = new ArrayList<ItemType>();
        for (PatternType type : this.patternTypes) {
            Material material = bannerMaterials.get(type);
            ItemType itemType = new ItemType(material);
            itemTypes.add(itemType);
        }
        return itemTypes.toArray(new ItemType[0]);
    }

    @Override
    public boolean isSingle() {
        return this.literalPattern.isSingle();
    }

    @Override
    public Class<ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.literalPattern.toString(event, debug) + " items";
    }

    @Nullable
    private static Material getMaterial(Object object) {
        if (!(object instanceof PatternType)) {
            return null;
        }
        PatternType patternType = (PatternType)object;
        String key = null;
        try {
            if (PATTERN_TYPE_IS_REGISTRY) {
                NamespacedKey namespacedKey = (NamespacedKey)PatternType.class.getMethod("getKey", new Class[0]).invoke((Object)patternType, new Object[0]);
                if (namespacedKey != null) {
                    key = namespacedKey.getKey().toUpperCase(Locale.ENGLISH);
                }
            } else {
                key = (String)PatternType.class.getMethod("toString", new Class[0]).invoke((Object)patternType, new Object[0]);
            }
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        if (key == null) {
            return null;
        }
        Material material = Material.getMaterial((String)(key + "_BANNER_PATTERN"));
        return material != null ? material : ExprBannerItem.checkAlias(patternType);
    }

    @Nullable
    private static Material checkAlias(PatternType patternType) {
        if (patternType == PatternType.BRICKS && Material.getMaterial((String)"FIELD_MASONED_BANNER_PATTERN") != null) {
            return Material.FIELD_MASONED_BANNER_PATTERN;
        }
        if (patternType == PatternType.BORDER && Material.getMaterial((String)"BORDURE_INDENTED_BANNER_PATTER") != null) {
            return Material.BORDURE_INDENTED_BANNER_PATTERN;
        }
        return null;
    }

    static {
        Object[] bannerPatterns;
        bannerMaterials = new HashMap<Object, Material>();
        PATTERN_TYPE_IS_REGISTRY = false;
        Registry patternRegistry = Bukkit.getRegistry(PatternType.class);
        if (patternRegistry != null) {
            bannerPatterns = patternRegistry.stream().toArray();
            PATTERN_TYPE_IS_REGISTRY = true;
        } else {
            try {
                Class<?> patternClass = Class.forName("org.bukkit.block.banner.PatternType");
                if (!patternClass.isEnum()) {
                    throw new IllegalStateException("PatternType is neither an enum nor a valid registry.");
                }
                Class<?> enumClass = patternClass;
                bannerPatterns = enumClass.getEnumConstants();
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        if (bannerPatterns != null) {
            for (Object object : bannerPatterns) {
                Material material = ExprBannerItem.getMaterial(object);
                if (material == null) continue;
                bannerMaterials.put(object, material);
            }
            Skript.registerExpression(ExprBannerItem.class, ItemType.class, ExpressionType.COMBINED, "[a[n]] %*bannerpatterntypes% item[s]");
        }
    }
}

