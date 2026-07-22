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

@Name(value="With Fire Resistance")
@Description(value={"Creates a copy of an item with (or without) fire resistance."})
@Example.Examples(value={@Example(value="set {_x} to diamond sword with fire resistance"), @Example(value="equip player with netherite helmet without fire resistance"), @Example(value="drop fire resistant stone at player")})
@RequiredPlugins(value={"Spigot 1.20.5+"})
@Since(value={"2.9.0"})
public class ExprWithFireResistance
extends PropertyExpression<ItemType, ItemType> {
    private boolean out;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.out = parseResult.hasTag("out");
        return true;
    }

    protected ItemType[] get(Event event, ItemType[] source) {
        return this.get((ItemType[])source.clone(), item -> {
            ItemMeta meta = item.getItemMeta();
            meta.setFireResistant(!this.out);
            item.setItemMeta(meta);
            return item;
        });
    }

    @Override
    public Class<? extends ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.getExpr().toString(event, debug) + " with fire resistance";
    }

    static {
        if (Skript.methodExists(ItemMeta.class, "setFireResistant", Boolean.TYPE)) {
            Skript.registerExpression(ExprWithFireResistance.class, ItemType.class, ExpressionType.PROPERTY, "%itemtype% with[:out] fire[ ]resistance", "fire resistant %itemtype%");
        }
    }
}

