/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.components.CustomModelDataComponent
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.Locale;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

@Name(value="Has Custom Model Data")
@Description(value={"Check if an item has a custom model data tag"})
@Example.Examples(value={@Example(value="player's tool has custom model data"), @Example(value="if player's tool has custom model data flags:\n\tloop custom model data flags of player's tool:\n\t\tsend \"Flag %loop-index%: %loop-value%\"\n"), @Example(value="set {_coloured} to whether player's tool has model data colours")})
@Since(value={"2.5, 2.12 (expanded data types)"})
@RequiredPlugins(value={"Minecraft 1.21.4+ (floats/flags/strings/colours)"})
public class CondHasCustomModelData
extends PropertyCondition<ItemType> {
    private static final boolean HAS_HAS_COMPONENT = Skript.methodExists(ItemMeta.class, "hasCustomModelDataComponent", new Class[0]);
    private CMDType dataType;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.dataType = CMDType.values()[parseResult.mark];
        return super.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public boolean check(ItemType item) {
        ItemMeta meta = item.getItemMeta();
        if (this.dataType == CMDType.ANY) {
            return HAS_HAS_COMPONENT ? meta.hasCustomModelDataComponent() : meta.hasCustomModelData();
        }
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        return switch (this.dataType.ordinal()) {
            default -> throw new MatchException(null, null);
            case 1 -> {
                if (!component.getFloats().isEmpty()) {
                    yield true;
                }
                yield false;
            }
            case 2 -> {
                if (!component.getFlags().isEmpty()) {
                    yield true;
                }
                yield false;
            }
            case 3 -> {
                if (!component.getStrings().isEmpty()) {
                    yield true;
                }
                yield false;
            }
            case 4 -> {
                if (!component.getColors().isEmpty()) {
                    yield true;
                }
                yield false;
            }
            case 0 -> throw new IllegalStateException("Wrong path for CMDType.ANY.");
        };
    }

    @Override
    protected PropertyCondition.PropertyType getPropertyType() {
        return PropertyCondition.PropertyType.HAVE;
    }

    @Override
    protected String getPropertyName() {
        return "custom model data" + (String)(this.dataType != CMDType.ANY ? " " + this.dataType.name().toLowerCase(Locale.ENGLISH) : "");
    }

    static {
        if (Skript.methodExists(ItemMeta.class, "getCustomModelDataComponent", new Class[0])) {
            CondHasCustomModelData.register(CondHasCustomModelData.class, PropertyCondition.PropertyType.HAVE, "[custom] model data [1:floats|2:flags|3:strings|4:colo[u]rs]", "itemtypes");
        } else {
            CondHasCustomModelData.register(CondHasCustomModelData.class, PropertyCondition.PropertyType.HAVE, "[custom] model data", "itemtypes");
        }
    }

    private static enum CMDType {
        ANY,
        FLOATS,
        FLAGS,
        STRINGS,
        COLORS;

    }
}

