/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Item with Enchantment Glint")
@Description(value={"Get an item with or without enchantment glint."})
@Example.Examples(value={@Example(value="set {_item with glint} to diamond with enchantment glint"), @Example(value="set {_item without glint} to diamond without enchantment glint")})
@RequiredPlugins(value={"Spigot 1.20.5+"})
@Since(value={"2.10"})
public class ExprItemWithEnchantmentGlint
extends PropertyExpression<ItemType, ItemType> {
    private boolean glint;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(expressions[0]);
        this.glint = !parseResult.hasTag("out");
        return true;
    }

    protected ItemType[] get(Event event, ItemType[] source) {
        return this.get(source, itemType -> {
            itemType = itemType.clone();
            ItemMeta meta = itemType.getItemMeta();
            meta.setEnchantmentGlintOverride(Boolean.valueOf(this.glint));
            itemType.setItemMeta(meta);
            return itemType;
        });
    }

    @Override
    public Class<? extends ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.getExpr().toString(event, debug) + (this.glint ? " with" : " without") + " enchantment glint";
    }

    static {
        if (Skript.methodExists(ItemMeta.class, "getEnchantmentGlintOverride", new Class[0])) {
            Skript.registerExpression(ExprItemWithEnchantmentGlint.class, ItemType.class, ExpressionType.PROPERTY, "%itemtypes% with[:out] [enchant[ment]] glint");
        }
    }
}

