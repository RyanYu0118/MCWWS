/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Hotbar Button")
@Description(value={"The hotbar button clicked in an <a href='#inventory_click'>inventory click</a> event."})
@Example(value="on inventory click:\n\tsend \"You clicked the hotbar button %hotbar button%!\"\n")
@Since(value={"2.5"})
public class ExprHotbarButton
extends SimpleExpression<Long>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(InventoryClickEvent.class);
    }

    @Nullable
    protected Long[] get(Event e) {
        if (e instanceof InventoryClickEvent) {
            return new Long[]{((InventoryClickEvent)e).getHotbarButton()};
        }
        return null;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the hotbar button";
    }

    static {
        Skript.registerExpression(ExprHotbarButton.class, Long.class, ExpressionType.SIMPLE, "[the] hotbar button");
    }
}

