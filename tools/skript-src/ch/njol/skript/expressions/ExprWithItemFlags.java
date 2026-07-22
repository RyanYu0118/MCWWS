/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.meta.ItemMeta
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
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Item with Item Flags")
@Description(value={"Creates a new item with the specified item flags."})
@Example.Examples(value={@Example(value="give player diamond sword with item flags hide enchants and hide attributes"), @Example(value="set {_item} to player's tool with item flag hide additional tooltip"), @Example(value="give player torch with hide placed on item flag"), @Example(value="set {_item} to diamond sword with all item flags")})
@Since(value={"2.10, 2.11 (all itemflags)"})
public class ExprWithItemFlags
extends SimpleExpression<ItemType> {
    private Expression<ItemFlag> itemFlags;
    private Expression<ItemType> itemTypes;
    private boolean allFlags;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.itemTypes = exprs[0];
        if (matchedPattern <= 1) {
            this.itemFlags = exprs[1];
        }
        this.allFlags = matchedPattern == 2;
        return true;
    }

    protected ItemType[] get(Event event) {
        ItemType[] types = this.itemTypes.getArray(event);
        ItemFlag[] flags = this.allFlags ? ItemFlag.values() : this.itemFlags.getArray(event);
        ItemType[] result = new ItemType[types.length];
        for (int i = 0; i < types.length; ++i) {
            ItemType clonedType = types[i].clone();
            ItemMeta meta = clonedType.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(flags);
                clonedType.setItemMeta(meta);
            }
            result[i] = clonedType;
        }
        return result;
    }

    @Override
    public boolean isSingle() {
        return this.itemTypes.isSingle();
    }

    @Override
    public Class<? extends ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.allFlags) {
            return this.itemTypes.toString(event, debug) + " with all item flags";
        }
        return this.itemTypes.toString(event, debug) + " with item flags " + this.itemFlags.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprWithItemFlags.class, ItemType.class, ExpressionType.COMBINED, "%itemtypes% with [the] item flag[s] %itemflags%", "%itemtypes% with [the] %itemflags% item flag[s]", "%itemtypes% with all [the] item flags");
    }
}

