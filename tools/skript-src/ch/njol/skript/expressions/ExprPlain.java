/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Plain Item")
@Description(value={"A plain item is an item with no modifications. It can be used to convert items to their default state or to match with other default items."})
@Example(value="if the player's tool is a plain diamond: # check if player's tool has no modifications\n\tsend \"You are holding a plain diamond!\"\n")
@Since(value={"2.6"})
public class ExprPlain
extends SimpleExpression<ItemType> {
    private Expression<ItemType> item;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.item = exprs[0];
        return true;
    }

    @Nullable
    protected ItemType[] get(Event e) {
        ItemType itemType = this.item.getSingle(e);
        if (itemType == null) {
            return new ItemType[0];
        }
        return new ItemType[]{itemType.getPlainType()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "plain " + this.item.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprPlain.class, ItemType.class, ExpressionType.COMBINED, "[a[n]] (plain|unmodified) %itemtype%");
    }
}

