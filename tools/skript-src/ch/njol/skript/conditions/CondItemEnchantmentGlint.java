/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.meta.ItemMeta
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
import org.bukkit.inventory.meta.ItemMeta;

@Name(value="Item Has Enchantment Glint Override")
@Description(value={"Checks whether an item has the enchantment glint overridden, or is forced to glint or not."})
@Example.Examples(value={@Example(value="if the player's tool has the enchantment glint override\n\tsend \"Your tool has the enchantment glint override.\" to player\n"), @Example(value="if {_item} is forced to glint:\n\tsend \"This item is forced to glint.\" to player\nelse if {_item} is forced to not glint:\n\tsend \"This item is forced to not glint.\" to player\nelse:\n\tsend \"This item does not have any glint override.\" to player\n")})
@RequiredPlugins(value={"Spigot 1.20.5+"})
@Since(value={"2.10"})
public class CondItemEnchantmentGlint
extends PropertyCondition<ItemType> {
    private int matchedPattern;
    private boolean glint;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.matchedPattern = matchedPattern;
        this.glint = !parseResult.hasTag("not");
        return super.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public boolean check(ItemType itemType) {
        ItemMeta meta = itemType.getItemMeta();
        if (this.matchedPattern == 0) {
            return meta.hasEnchantmentGlintOverride();
        }
        if (!meta.hasEnchantmentGlintOverride()) {
            return false;
        }
        return meta.getEnchantmentGlintOverride();
    }

    @Override
    protected String getPropertyName() {
        if (this.matchedPattern == 0) {
            return "enchantment glint overridden";
        }
        return "forced to " + (this.glint ? "" : "not ") + "glint";
    }

    static {
        if (Skript.methodExists(ItemMeta.class, "getEnchantmentGlintOverride", new Class[0])) {
            CondItemEnchantmentGlint.register(CondItemEnchantmentGlint.class, PropertyCondition.PropertyType.HAVE, "enchantment glint overrid(den|e)", "itemtypes");
            CondItemEnchantmentGlint.register(CondItemEnchantmentGlint.class, PropertyCondition.PropertyType.BE, "forced to [:not] glint", "itemtypes");
        }
    }
}

